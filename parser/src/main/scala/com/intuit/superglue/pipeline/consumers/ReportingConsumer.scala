package com.intuit.superglue.pipeline.consumers

import java.time.LocalDateTime

import com.intuit.superglue.pipeline.Implicits.StringEtc
import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.consumers.OutputConsumer.{EndOfStream, Message, StartOfStream}
import com.intuit.superglue.pipeline.consumers.ReportingConsumer._
import com.typesafe.config.{Config => TypesafeConfig}
import play.api.libs.json._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ReportingConsumer(
  private val sanitizedConfig: Option[JsValue] = None,
)(implicit val rootConfig: TypesafeConfig)
  extends OutputConsumer[Future[ScriptMetadata]] {
  private val reporterConfig = rootConfig.getConfig("com.intuit.superglue.pipeline.outputs.reporter")
  private val enabled = reporterConfig.getBoolean("enabled")
  private val errorsOnly = reporterConfig.getBoolean("errors-only")

  private var executionStartTime: Option[LocalDateTime] = None
  private var executionEndTime: Option[LocalDateTime] = None

  private var futureScriptReports = Seq.empty[Future[(ScriptReport, Map[String, BreakdownReportElement])]]
  private var report: Option[Report] = Option.empty[Report]

  override def accept(event: OutputConsumer.Event[Future[ScriptMetadata]]): Unit = {
    if (!enabled) return
    event match {
      case StartOfStream(startTime) => executionStartTime = Some(startTime)
      case Message(futureMetadata) =>
        val futureScriptReport = futureMetadata.map { scriptMetadata =>
          // Create a report from each script's metadata
          val scriptReport = ScriptReport(scriptMetadata)

          // Count the number of each type of statement encountered
          val statementBreakdowns = scriptReport.statementReports.map { statementReport =>
            val didError = statementReport.statementErrors.nonEmpty
            val stmtType = statementReport.statementType
            stmtType -> BreakdownReportElement(1, if (didError) 1 else 0)
          }.toMap

          (scriptReport, statementBreakdowns)
        }

        futureScriptReports :+= futureScriptReport

      case EndOfStream =>
        val futureReports = Future.sequence(futureScriptReports)
        val reportSeq = Await.result(futureReports, 60 second)
        val scriptReports = reportSeq.map { case (scriptReport, _) => scriptReport }

        val executionScriptsReceived = scriptReports.size
        val executionScriptsFailed = scriptReports.count(_.scriptErrors.nonEmpty)
        val executionStatementsReceived = scriptReports.map(_.statementCount).sum
        val executionStatementsFailed = scriptReports.map(_.statementFailedCount).sum

        val statementBreakdownReport = reportSeq.map { case (_, breakdown) => breakdown }
            .fold(Map.empty[String, BreakdownReportElement])((one, two) => (one.keySet ++ two.keySet)
              .map(key => (key, one.getOrElse(key, BreakdownReportElement()) + two.getOrElse(key, BreakdownReportElement()))).toMap)

        executionEndTime = Some(LocalDateTime.now())
        val executionReport = ExecutionReport(
          executionStartTime.get,
          executionEndTime.get,
          sanitizedConfig,
          executionScriptsReceived,
          executionScriptsFailed,
          executionStatementsReceived,
          executionStatementsFailed,
        )
        report = Some(Report(executionReport, statementBreakdownReport, scriptReports))
    }
  }

  def reportJson(errorOnly: Boolean = errorsOnly): Option[JsValue] = {
    report.map { report =>
      val strippedReport = if (!errorOnly) report else {
        report.copy(
          scriptReports = report.scriptReports.filter { scriptReport =>
            scriptReport.scriptErrors.nonEmpty || scriptReport.statementFailedCount > 0
          }
        )
      }
      Json.toJson(strippedReport)
    }
  }
}

object ReportingConsumer {

  case class InnerConfig(
    errorsOnly: Boolean,
  )

  case class Report(
    executionReport: ExecutionReport,
    statementBreakdown: Map[String, BreakdownReportElement],
    scriptReports: Seq[ScriptReport],
  )

  object Report {
    implicit val reportFormat: OFormat[Report] = Json.format[Report]
  }

  case class ExecutionReport(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    configuration: Option[JsValue],
    scriptsReceived: Int,
    scriptsFailed: Int,
    statementsReceived: Int,
    statementsFailed: Int,
  )

  object ExecutionReport {
    implicit val executionReportFormat: OFormat[ExecutionReport] = Json.format[ExecutionReport]
  }

  case class ScriptReport(
    scriptName: String,
    scriptSource: String,
    scriptKind: String,
    scriptParser: String,
    scriptParseStartTime: LocalDateTime,
    scriptParseEndTime: LocalDateTime,
    scriptErrors: List[String],
    statementCount: Int,
    statementFailedCount: Int,
    statementReports: List[StatementReport],
  )

  object ScriptReport {
    private[pipeline] def apply(scriptMetadata: ScriptMetadata): ScriptReport = {
      val statementReports = scriptMetadata.statementsMetadata.map(StatementReport(_))
      ScriptReport(
        scriptMetadata.scriptName,
        scriptMetadata.scriptSource,
        scriptMetadata.scriptKind,
        scriptMetadata.scriptParser,
        scriptMetadata.scriptParseStartTime,
        scriptMetadata.scriptParseEndTime,
        scriptMetadata.errors.map(_.toString),
        scriptMetadata.statementsMetadata.size,
        statementReports.count(_.statementErrors.nonEmpty),
        statementReports,
      )
    }

    implicit val scriptReport: OFormat[ScriptReport] = Json.format[ScriptReport]
  }

  case class StatementReport(
    statementParseStartTime: LocalDateTime,
    statementParseEndTime: LocalDateTime,
    statementPreview: String,
    statementType: String,
    statementErrors: List[String],
  )

  object StatementReport {
    private[pipeline] def apply(statementMetadata: StatementMetadata): StatementReport = StatementReport(
      statementMetadata.statementParseStartTime,
      statementMetadata.statementParseEndTime,
      statementMetadata.statementText etc 80,
      statementMetadata.statementMetadataFragment.statementType,
      statementMetadata.statementMetadataFragment.errors.map(_.toString)
    )

    implicit val statementReport: OFormat[StatementReport] = Json.format[StatementReport]
  }

  case class BreakdownReportElement(
    totalOccurrences: Int = 0,
    failedOccurrences: Int = 0,
  ) {
    def +(other: BreakdownReportElement) = BreakdownReportElement(
      totalOccurrences + other.totalOccurrences,
      failedOccurrences + other.failedOccurrences,
    )
  }

  object BreakdownReportElement {
    implicit val breakdownReportElement: OFormat[BreakdownReportElement] = Json.format[BreakdownReportElement]
  }
}
