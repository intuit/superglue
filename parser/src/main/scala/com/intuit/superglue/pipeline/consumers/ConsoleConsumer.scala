package com.intuit.superglue.pipeline.consumers

import java.io.PrintStream

import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.consumers.ConsoleConsumer.ScriptView
import com.intuit.superglue.pipeline.consumers.OutputConsumer.{EndOfStream, Message, StartOfStream}
import com.typesafe.config.{Config => TypesafeConfig}
import play.api.libs.json.{JsValue, Json, OFormat}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ConsoleConsumer(implicit out: PrintStream, rootConfig: TypesafeConfig) extends OutputConsumer[Future[ScriptMetadata]] {
  private val consoleConfig = rootConfig.getConfig("com.intuit.superglue.pipeline.outputs.console")
  private val enabled = consoleConfig.getBoolean("enabled")
  private var printObjects: Seq[Future[Option[ScriptView]]] = List.empty[Future[Option[ScriptView]]]

  override def accept(event: OutputConsumer.Event[Future[ScriptMetadata]]): Unit = {
    if (!enabled) return
    event match {
      case StartOfStream(_) => // We don't need to print the start time to the console
      case Message(futureMetadata) =>
        printObjects :+= futureMetadata.map(ConsoleConsumer.ScriptView(_))
      case EndOfStream =>
        out.println(Json.prettyPrint(scriptJson(printObjects)))
    }
  }

  private def scriptJson(scriptViews: Seq[Future[Option[ScriptView]]]): JsValue = {
    val futureViews = Future.sequence(scriptViews)
    val result = futureViews.map { maybeViews =>
      maybeViews.collect { case Some(view) => view }
    }
    val views = Await.result(result, 60 second)
    Json.toJson(views)
  }
}

object ConsoleConsumer {

  case class ScriptView(
    name: String,
    statements: Seq[StatementView],
  )

  object ScriptView {
    def apply(scriptMetadata: ScriptMetadata, filterUnknown: Boolean = true): Option[ScriptView] = {
      val scriptView = ScriptView(
        scriptMetadata.scriptName,
        scriptMetadata.statementsMetadata.map(StatementView(_)).filter { statement =>
          statement.inputObjects.nonEmpty || statement.outputObjects.nonEmpty
        }
      )
      if (scriptView.statements.nonEmpty) Some(scriptView) else None
    }

    implicit val scriptFormat: OFormat[ScriptView] = Json.format[ScriptView]
  }

  case class StatementView(
    `type`: String,
    inputObjects: Seq[String],
    outputObjects: Seq[String],
  )

  object StatementView {
    def apply(statementMetadata: StatementMetadata, filterUnknown: Boolean = true): StatementView = {
      StatementView(
        statementMetadata.statementMetadataFragment.statementType,
        statementMetadata.statementMetadataFragment.inputObjects,
        statementMetadata.statementMetadataFragment.outputObjects,
      )
    }

    implicit val statementFormat: OFormat[StatementView] = Json.format[StatementView]
  }
}