package aiven.io.guardian.kafka

import aiven.io.guardian.kafka.configs.KafkaCluster
import pureconfig.generic.auto._
import pureconfig.ConfigSource

import scala.annotation.nowarn

trait Config {

  @nowarn("cat=lint-byname-implicit")
  implicit lazy val kafkaClusterConfig: KafkaCluster =
    ConfigSource.default.at("kafka-cluster").loadOrThrow[KafkaCluster]
}

object Config extends Config
