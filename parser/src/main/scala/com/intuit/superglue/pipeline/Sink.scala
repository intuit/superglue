package com.intuit.superglue.pipeline

import java.io.PrintStream
import java.time.LocalDateTime

import Metadata.ScriptMetadata
import com.intuit.superglue.pipeline.consumers.OutputConsumer.{EndOfStream, Message, StartOfStream}
import com.intuit.superglue.pipeline.consumers.{ConsoleConsumer, DatabaseConsumer, OutputConsumer}
import com.typesafe.config.{Config => TypesafeConfig}

import scala.concurrent.Future

class Sink(
  pipeline: ParsingPipeline,
  customConsumers: (String, OutputConsumer[Future[ScriptMetadata]])*,
)(implicit out: PrintStream, rootConfig: TypesafeConfig) {
  private lazy val consumers: Map[String, OutputConsumer[Future[ScriptMetadata]]] = Map(
    "console" -> new ConsoleConsumer(),
    "database" -> new DatabaseConsumer(),
  ) ++ customConsumers.toMap

  /**
    * Sends each [[ScriptMetadata]] object as an event to each [[OutputConsumer]].
    *
    * Outputs are sent as a payload in a [[Message]], and after all outputs
    * have been delivered, an [[EndOfStream]] event is sent so that each
    * consumer can flush messages and release resources.
    */
  def drain(): Map[String, OutputConsumer[Future[ScriptMetadata]]] = {
    val startTime = LocalDateTime.now()
    Iterator(
      Iterator(StartOfStream(startTime)),
      pipeline.stream().map(Message(_)),
      Iterator(EndOfStream)
    ).flatten.foreach(metadata => consumers.values.foreach(_.accept(metadata)))
    consumers
  }
}
