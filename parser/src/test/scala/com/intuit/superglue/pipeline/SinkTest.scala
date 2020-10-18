package com.intuit.superglue.pipeline

import java.io.PrintStream
import java.time.LocalDateTime

import com.intuit.superglue.pipeline.Metadata.ScriptMetadata
import com.intuit.superglue.pipeline.consumers.{ConsoleConsumer, DatabaseConsumer, OutputConsumer}
import com.intuit.superglue.pipeline.consumers.OutputConsumer.{Event, Message}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class SinkTest extends FlatSpec {
  private val config = ConfigFactory.parseString(
    """
      |com.intuit.superglue {
      |  dao {
      |    backend = "relational"
      |    relational {
      |      profile = "slick.jdbc.MySQLProfile$"
      |      db {
      |        url = ""
      |        user = ""
      |        password = ""
      |      }
      |    }
      |  }
      |  pipeline {
      |    outputs.console.enabled = false
      |    outputs.database {
      |      enabled = false
      |      batch-size = 50
      |      timeout = 10
      |    }
      |  }
      |}
    """.stripMargin)

  "A Sink" should "send all events to one consumer" in {
    implicit val out: PrintStream = System.out
    implicit val rootConfig: Config = config
    val testTime = LocalDateTime.now()
    val mockPipeline = new ParsingPipeline(null) {
      override def stream(): Iterator[Future[ScriptMetadata]] = Iterator(Future(
        ScriptMetadata(
          scriptName = "test script",
          scriptSource = "test",
          scriptKind = "SQL",
          scriptDialect = None,
          scriptParser = "TestParser",
          scriptParseStartTime = testTime,
          scriptParseEndTime = testTime,
          statementsMetadata = List.empty,
          errors = List.empty,
        )
      ))
    }

    val nopConsumer = new OutputConsumer[Future[ScriptMetadata]] {
      override def accept(event: Event[Future[ScriptMetadata]]): Unit = ()
    }

    var futureReceived = Seq.empty[Event[Future[ScriptMetadata]]]
    val mockConsumer = new OutputConsumer[Future[ScriptMetadata]] {
      override def accept(event: Event[Future[ScriptMetadata]]): Unit = {
        futureReceived :+= event
      }
    }

    val sink = new Sink(mockPipeline,
      "console" -> mockConsumer,
      "database" -> nopConsumer,
    )

    // The sink should return all of the consumers after draining
    val outputConsumers = sink.drain()
    assert(outputConsumers.values.toSet.contains(mockConsumer))
    assert(outputConsumers.values.toSet.contains(nopConsumer))

    val futureMetadata = futureReceived.collect { case Message(metadata) => metadata }
    val received = Await.result(Future.sequence(futureMetadata), 2 second)
    assert(received.contains(
      ScriptMetadata(
        scriptName = "test script",
        scriptSource = "test",
        scriptKind = "SQL",
        scriptDialect = None,
        scriptParser = "TestParser",
        scriptParseStartTime = testTime,
        scriptParseEndTime = testTime,
        statementsMetadata = List.empty,
        errors = List.empty,
      ))
    )
  }

  it should "instantiate the default consumers" in {
    implicit val out: PrintStream = System.out
    implicit val rootConfig: Config = config
    val nopPipeline = new ParsingPipeline(null) {
      override def stream(): Iterator[Future[ScriptMetadata]] = Iterator()
    }
    val sink = new Sink(nopPipeline)
    val consumers = sink.drain()
    assert(consumers.values.exists(consumer => consumer.isInstanceOf[ConsoleConsumer]))
    assert(consumers.values.exists(consumer => consumer.isInstanceOf[DatabaseConsumer]))
  }
}
