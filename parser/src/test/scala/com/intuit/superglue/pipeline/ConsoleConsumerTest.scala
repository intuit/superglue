package com.intuit.superglue.pipeline

import java.io.{ByteArrayOutputStream, PrintStream}
import java.time.LocalDateTime

import com.intuit.superglue.pipeline.Metadata.{ScriptMetadata, StatementMetadata, StatementMetadataFragment}
import com.intuit.superglue.pipeline.consumers.ConsoleConsumer
import com.intuit.superglue.pipeline.consumers.OutputConsumer.{EndOfStream, Message, StartOfStream}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.FlatSpec
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConsoleConsumerTest extends FlatSpec {

  "A Console Consumer" should "print the metadata of scripts" in {

    implicit val rootConfig: Config = ConfigFactory.parseString(
      """
        |com.intuit.superglue.pipeline {
        |  outputs.console {
        |    enabled = true
        |    errors-only = true
        |  }
        |}
      """.stripMargin)
    val buffer = new ByteArrayOutputStream()
    implicit val printer: PrintStream = new PrintStream(buffer)
    val consoleConsumer = new ConsoleConsumer()

    // Mock some data to send to the console
    val testTime = LocalDateTime.now()
    val testStatementMetadata = StatementMetadata(
      statementText = "Test statement text",
      statementIndex = 0,
      statementParseStartTime = testTime,
      statementParseEndTime = testTime,
      statementMetadataFragment = StatementMetadataFragment(
        statementParser = "TestStatementParser",
        statementType = "TestStatementType",
        inputObjects = List("TestInputObject"),
        outputObjects = List("TestOutputObject"),
        errors = List(
          new Exception("Test statement error 1"),
          new Exception("Test statement error 2"),
        )
      ),
    )

    val testScriptMetadata = ScriptMetadata(
      scriptName = "One",
      scriptSource = "TestSource",
      scriptKind = "TestKind",
      scriptDialect = None,
      scriptParser = getClass.getName,
      scriptParseStartTime = testTime,
      scriptParseEndTime = testTime,
      statementsMetadata = List(
        testStatementMetadata,
        testStatementMetadata
      ),
      errors = List(
        new Exception("Test script error 1"),
        new Exception("Test script error 2"),
      ),
    )

    val events = List(
      testScriptMetadata,
      testScriptMetadata.copy(
        scriptName = "Two",
        statementsMetadata = List(
          testStatementMetadata.copy(
            statementMetadataFragment = testStatementMetadata.statementMetadataFragment
              .copy(errors = List.empty[Throwable])
          )
        ),
        errors = List.empty[Throwable],
      ),
    )

    // Send the metadata to the reporter
    consoleConsumer.accept(StartOfStream(testTime))
    events.map(Future(_)).map(Message(_)).foreach(consoleConsumer.accept(_))
    consoleConsumer.accept(EndOfStream)

    val outputString = buffer.toString()
    assert(!outputString.isEmpty)

    // Assert that the output parses into json
    val json = Json.parse(outputString)
    assert((json \ 0 \ "name").isDefined)
    assert((json \ 0 \ "statements").isDefined)
    assert((json \ 0 \ "statements" \ 0 \ "type").isDefined)
    assert((json \ 0 \ "statements" \ 0 \ "inputObjects").isDefined)
    assert((json \ 0 \ "statements" \ 0 \ "outputObjects").isDefined)
  }
}
