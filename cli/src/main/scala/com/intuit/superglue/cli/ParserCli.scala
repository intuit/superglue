package com.intuit.superglue.cli

import java.io.PrintStream
import java.nio.file.{FileSystem, Files, Path}

import com.intuit.superglue.pipeline.consumers.ReportingConsumer
import com.intuit.superglue.pipeline.parsers.{CalciteStatementParser, SqlScriptParser}
import com.intuit.superglue.pipeline.{ParsingPipeline, Sink, Source}
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory, Config => TypesafeConfig}
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec
import play.api.libs.json._

import scala.annotation.meta.field

@CommandLine.Command(name = "parse",
  description = Array("Parses SQL files to generate lineage"))
class ParserCli(implicit out: PrintStream, fs: FileSystem)
  extends Runnable {

  @(CommandLine.Spec@field)
  var spec: CommandSpec = _

  @(CommandLine.Option@field)(
    names = Array("-c", "--config"),
    description = Array("A custom config file to use for execution"))
  var config: Path = fs.getPath("./superglue.conf")

  @(CommandLine.Option@field)(
    names = Array("--printInputs"),
    description = Array("Print discovered inputs, but do not parse"))
  var printInputs: Boolean = false

  @(CommandLine.Option@field)(
    names = Array("--report"),
    description = Array("Print a JSON report about this execution"))
  var report: Boolean = false

  override def run(): Unit = {

    // Workaround: Picocli always uses the default Filesystem, rather than the custom
    // one provided by the "fs" parameter (needed for testing)
    // We just stringify the path and then resolve it on the fs we want
    val configFile: Path = fs.getPath(config.toString)

    // If the given (or default) configuration is not found, print usage and quit
    if (!Files.exists(configFile)) {
      out.println(s"Configuration file '${configFile.toString}' not found")
      new CommandLine(spec).usage(out)
      return
    }

    var conf: TypesafeConfig = ConfigFactory
      .parseReader(Files.newBufferedReader(configFile))
      .withFallback(ConfigFactory.load())

    // If the --report switch was given, enable reporter and disable console in the configuration
    if (report) {
      conf = conf.withValue("com.intuit.superglue.pipeline.outputs.reporter.enabled", ConfigValueFactory.fromAnyRef(true))
      conf = conf.withValue("com.intuit.superglue.pipeline.outputs.console.enabled", ConfigValueFactory.fromAnyRef(false))
    }

    implicit val rootConfig: TypesafeConfig = conf

    // If the --printInputs switch was given, print the names of each input and quit
    if (printInputs) {
      new Source().stream().foreach(input => out.println(input.name))
      return
    }

    val source = new Source()
    val pipeline = new ParsingPipeline(source, "sql" -> new SqlScriptParser(statementParser = new CalciteStatementParser()))
    val sanitizedConfig = configAsSanitizedJs(rootConfig)
    val sink = new Sink(pipeline, "reporter" -> new ReportingConsumer(sanitizedConfig))
    val consumers = sink.drain()
    val reporter = consumers("reporter").asInstanceOf[ReportingConsumer]

    if (report) {
      out.println(Json.prettyPrint(reporter.reportJson().get))
    }
  }

  def configAsSanitizedJs(config: TypesafeConfig): Option[JsValue] = {
    val jsConfig = Json.parse(config.getConfig("com.intuit.superglue").root.render(ConfigRenderOptions.concise()))
    val noUrl = (__ \ "dao" \ "relational" \ "db" \ "url").json.prune
    val noUser = (__ \ "dao" \ "relational" \ "db" \ "user").json.prune
    val noPassword = (__ \ "dao" \ "relational" \ "db" \ "password").json.prune
    jsConfig.transform(noUrl.andThen(noUser).andThen(noPassword)).asOpt
  }
}
