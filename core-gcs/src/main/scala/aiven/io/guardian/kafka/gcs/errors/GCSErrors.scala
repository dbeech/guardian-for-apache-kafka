package aiven.io.guardian.kafka.gcs.errors

import aiven.io.guardian.kafka.Errors

sealed abstract class GCSErrors extends Errors

object GCSErrors {
  final case class ExpectedObjectToExist(bucketName: String, maybePrefix: Option[String]) extends GCSErrors {
    override def getMessage: String =
      ???
  }

}
