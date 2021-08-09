package aiven.io.guardian.kafka.backup.s3

import aiven.io.guardian.kafka.KafkaClientInterface
import aiven.io.guardian.kafka.backup.BackupClientInterface
import aiven.io.guardian.kafka.backup.configs.Backup
import aiven.io.guardian.kafka.s3.configs.{S3 => S3Config}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{MultipartUploadResult, S3Headers}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future

class BackupClient(s3Headers: S3Headers)(implicit
    override val kafkaClientInterface: KafkaClientInterface,
    override val backupConfig: Backup,
    s3Config: S3Config
) extends BackupClientInterface {
  override type BackupResult = MultipartUploadResult

  override def backupToStorageSink(key: String): Sink[ByteString, Future[BackupResult]] =
    S3.multipartUploadWithHeaders(
      s3Config.dataBucket,
      key,
      s3Headers = s3Headers
    )
}