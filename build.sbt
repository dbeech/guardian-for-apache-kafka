import com.jsuereth.sbtpgp.PgpKeys.publishSigned

ThisBuild / scalaVersion         := "2.13.6"
ThisBuild / organization         := "aiven.io"
ThisBuild / organizationName     := "Aiven"
ThisBuild / organizationHomepage := Some(url("https://aiven.io/"))

val akkaVersion           = "2.6.15"
val alpakkaKafkaVersion   = "2.1.0"
val alpakkaVersion        = "3.0.1"
val quillJdbcMonixVersion = "3.7.2"
val postgresqlJdbcVersion = "42.2.23"
val scalaLoggingVersion   = "3.9.4"
val logbackClassicVersion = "1.2.3"
val declineVersion        = "2.1.0"
val pureConfigVersion     = "0.16.0"
val scalaTestVersion      = "3.2.9"

val flagsFor12 = Seq(
  "-Xlint:_",
  "-Ywarn-infer-any",
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-opt-inline-from:<sources>",
  "-opt:l:method"
)

val flagsFor13 = Seq(
  "-Xlint:_",
  "-opt-inline-from:<sources>",
  "-opt:l:method"
)

val librarySettings = Seq(
  crossScalaVersions := List("2.12.14", "2.13.6"),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n == 13 =>
        flagsFor13
      case Some((2, n)) if n == 12 =>
        flagsFor12
    }
  }
)

val cliSettings = Seq(
  publishArtifact := false,
  scalacOptions ++= Seq(
    "-opt-inline-from:**", // See https://www.lightbend.com/blog/scala-inliner-optimizer
    "-opt:l:method"
  ),
  publish / skip       := true,
  publishLocal / skip  := true,
  publishSigned / skip := true
)

val baseName = "guardian-for-apache-kafka"

lazy val core = project
  .in(file("core"))
  .settings(
    librarySettings,
    name := s"$baseName-core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"          %% "akka-stream"       % akkaVersion,
      "com.typesafe.akka"          %% "akka-stream-kafka" % alpakkaKafkaVersion,
      "com.typesafe.scala-logging" %% "scala-logging"     % scalaLoggingVersion,
      "com.github.pureconfig"      %% "pureconfig"        % pureConfigVersion,
      "ch.qos.logback"              % "logback-classic"   % logbackClassicVersion
    )
  )

lazy val coreAws = project
  .in(file("core-s3"))
  .settings(
    librarySettings,
    name := s"$baseName-s3",
    libraryDependencies ++= Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % alpakkaVersion,
      "org.scalatest"      %% "scalatest"              % "3.2.9"   % Test,
      "org.scalatestplus"  %% "scalacheck-1-15"        % "3.2.9.0" % Test
    )
  )
  .dependsOn(core)

lazy val coreGcs = project
  .in(file("core-gcs"))
  .settings(
    librarySettings,
    name := s"$baseName-gcs",
    libraryDependencies ++= Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-google-cloud-storage" % alpakkaVersion
    )
  )
  .dependsOn(core)

lazy val backupS3 = project
  .in(file("backup-s3"))
  .settings(
    librarySettings,
    name := s"$baseName-backup-s3"
  )
  .dependsOn(coreAws)

lazy val backupGcs = project
  .in(file("backup-gcs"))
  .settings(
    librarySettings,
    name := s"$baseName-backup-gcs"
  )
  .dependsOn(coreGcs)

lazy val cliBackup = project
  .in(file("cli-backup"))
  .settings(
    cliSettings,
    name := s"$baseName-cli-backup",
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline" % declineVersion
    )
  )
  .dependsOn(backupS3, backupGcs)
  .enablePlugins(SbtNativePackager)

lazy val coreCompaction = project
  .in(file("core-compaction"))
  .settings(
    librarySettings,
    name := s"$baseName-core-compaction",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % postgresqlJdbcVersion
    )
  )
  .dependsOn(core)

lazy val compactionS3 = project
  .in(file("compaction-s3"))
  .settings(
    librarySettings,
    name := s"$baseName-compaction-s3"
  )
  .dependsOn(coreAws, coreCompaction)

lazy val compactionGcs = project
  .in(file("compaction-gcs"))
  .settings(
    librarySettings,
    name := s"$baseName-compaction-gcs"
  )
  .dependsOn(coreGcs, coreCompaction)

lazy val cliCompaction = project
  .in(file("cli-compaction"))
  .settings(
    cliSettings,
    name := s"$baseName-cli-compaction",
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline" % declineVersion
    )
  )
  .dependsOn(compactionS3, compactionGcs)
  .enablePlugins(SbtNativePackager)

lazy val restoreS3 = project
  .in(file("restore-s3"))
  .settings(
    librarySettings,
    name := s"$baseName-restore-s3"
  )
  .dependsOn(compactionS3)

lazy val restoreGcs = project
  .in(file("restore-gcs"))
  .settings(
    librarySettings,
    name := s"$baseName-restore-gcs"
  )
  .dependsOn(compactionGcs)

lazy val cliRestore = project
  .in(file("cli-restore"))
  .settings(
    cliSettings,
    name := s"$baseName-cli-restore",
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline" % declineVersion
    )
  )
  .dependsOn(restoreS3, restoreGcs)
  .enablePlugins(SbtNativePackager)

// This is currently causing problems, see https://github.com/djspiewak/sbt-github-actions/issues/74
ThisBuild / githubWorkflowUseSbtThinClient := false

ThisBuild / githubWorkflowPublishTargetBranches := Seq()

import ReleaseTransformations._

releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeReleaseAll"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)