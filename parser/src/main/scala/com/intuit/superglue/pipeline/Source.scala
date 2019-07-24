package com.intuit.superglue.pipeline

import java.nio.file.FileSystem

import com.intuit.superglue.pipeline.producers.{ScriptFileProvider, ScriptInput, ScriptProvider}
import com.typesafe.config.{Config => TypesafeConfig}

import scala.collection.JavaConverters._

/**
  * A Source is a helper class that takes a configuration and creates a stream of
  * all of the [[ScriptInput]]s from all of the described [[ScriptProvider]]s.
  */
class Source(customProviders: ScriptProvider*)
  (implicit rootConfig: TypesafeConfig, fs: FileSystem) {
  private val providers = if (customProviders.nonEmpty) customProviders else {
    val inputConfig = rootConfig.getConfig("com.intuit.superglue.pipeline.inputs")
    inputConfig.getConfigList("files").asScala
      .map(FileInputConfig(_))
      .collect { case Some(fileConfig) => ScriptFileProvider(fileConfig) }
  }

  def stream(): Iterator[ScriptInput] = providers.iterator.flatMap(_.stream())
}
