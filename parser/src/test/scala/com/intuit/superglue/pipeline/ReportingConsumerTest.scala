package com.intuit.superglue.pipeline

import java.time.LocalDateTime

import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.consumers.OutputConsumer.{EndOfStream, Message, StartOfStream}
import com.intuit.superglue.pipeline.consumers.ReportingConsumer
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import org.scalatest.FlatSpec
import play.api.libs.json.{JsDefined, JsNumber}

import scala.concurrent.Future

class ReportingConsumerTest extends FlatSpec {

  "A Reporting Consumer" should "count the number of scripts parsed" in {

    // Create a reporter instance to be tested
    implicit val rootConfig: TypesafeConfig = ConfigFactory.parseString(
      """
        |com.intuit.superglue.pipeline.outputs.reporter {
        |  enabled = true,
        |  errors-only = true,
        |}
      """.stripMargin)
    val reporter = new ReportingConsumer()

    // Mock some data to send to the reporter
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
    reporter.accept(StartOfStream(testTime))
    events.map(Future.successful).map(Message(_)).foreach(reporter.accept(_))
    reporter.accept(EndOfStream)

    val maybeJsonReport = reporter.reportJson(false)
    assert(maybeJsonReport.isDefined)
    val jsonReport = maybeJsonReport.get

    // Verify that the reporter counted two scripts
    val scriptsReceived = jsonReport \ "executionReport" \ "scriptsReceived"
    assert(scriptsReceived.isDefined)
    assert(scriptsReceived == JsDefined(JsNumber(2)))

    // Verify that the reporter counted one failed script
    val scriptsFailed = jsonReport \ "executionReport" \ "scriptsFailed"
    assert(scriptsFailed.isDefined)
    assert(scriptsFailed == JsDefined(JsNumber(1)))

    // Verify that the reporter counted three statements
    val statementsReceived = jsonReport \ "executionReport" \ "statementsReceived"
    assert(statementsReceived.isDefined)
    assert(statementsReceived == JsDefined(JsNumber(3)))

    // Verify that the reporter counted two failed statements
    val statementsFailed = jsonReport \ "executionReport" \ "statementsFailed"
    assert(statementsFailed.isDefined)
    assert(statementsFailed == JsDefined(JsNumber(2)))
  }
}
