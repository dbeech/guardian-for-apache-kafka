package io.aiven.guardian.kafka.backup.s3

import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.alpakka.s3.S3Attributes
import akka.stream.alpakka.s3.S3Headers
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl._
import akka.util.ByteString
import io.aiven.guardian.kafka.KafkaClientInterface
import io.aiven.guardian.kafka.backup.BackupClientInterface
import io.aiven.guardian.kafka.backup.configs.Backup
import io.aiven.guardian.kafka.s3.configs.{S3 => S3Config}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class BackupClient[T <: KafkaClientInterface](maybeS3Settings: Option[S3Settings])(implicit
    override val kafkaClientInterface: T,
    override val backupConfig: Backup,
    s3Config: S3Config,
    s3Headers: S3Headers
) extends BackupClientInterface[T] {

  override def empty: () => Future[Option[MultipartUploadResult]] = () => Future.successful(None)

  override type BackupResult = Option[MultipartUploadResult]

  override def backupToStorageSink(key: String): Sink[ByteString, Future[BackupResult]] = {
    val base = S3
      .multipartUploadWithHeaders(
        s3Config.dataBucket,
        key,
        s3Headers = s3Headers,
        chunkingParallelism = 1 // Parallelism is pointless for this usecase since we are directly streaming from Kafka
      )
      .mapMaterializedValue(future => future.map(result => Some(result))(ExecutionContext.parasitic))
    maybeS3Settings.fold(base)(s3Settings => base.withAttributes(S3Attributes.settings(s3Settings)))
  }
}
