package com.intuit.superglue

import com.typesafe.config.{Config => TypesafeConfig}
import pureconfig.generic.auto._

package object elastic {

  case class ElasticsearchConfig(
    hostname: String,
    port: Int,
    batchSize: Int,
    index: String,
    alias: String,
    indexType: String = "_doc",
  )

  object ElasticsearchConfig {
    private val schemePattern = "^https?://.*".r
    def apply(config: TypesafeConfig): Option[ElasticsearchConfig] = {
      pureconfig.loadConfig[ElasticsearchConfig](config).toOption
        .map(conf => conf.hostname match {
          case schemePattern() => conf
          case _ => conf.copy(hostname = s"http://${conf.hostname}")
        })
    }
  }
}
