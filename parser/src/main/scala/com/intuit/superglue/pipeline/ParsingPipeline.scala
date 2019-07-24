package com.intuit.superglue.pipeline

import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.parsers.{ScriptParser, SqlScriptParser}
import com.intuit.superglue.pipeline.producers.ScriptInput
import com.typesafe.config.{Config => TypesafeConfig}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

class ParsingPipeline(
  source: Source,
  customParsers: (String, ScriptParser)*,
)(implicit rootConfig: TypesafeConfig) {
  private val logger = Logger[ParsingPipeline]
  private lazy val parsers: Map[String, ScriptParser] = Map(
    "sql" -> new SqlScriptParser(),
  ) ++ customParsers.toMap

  /**
    * Transforms the stream of [[ScriptInput]]s from the [[Source]] into a stream of
    * [[ScriptMetadata]]s by passing each input to an accepting [[ScriptParser]].
    *
    * @return The stream of processed [[ScriptMetadata]] objects.
    */
  def stream(): Iterator[Future[ScriptMetadata]] = {
    source.stream()

      // For each input, find a parser that can parse that input kind
      .map(input => input -> checkParserKind(input.name, input.kind))

      // For each input-parser pair, use the parser on the input
      .collect {
        case (input, Some(parser)) => parser.parse(input)
      }
  }

  /**
    * Checks if there exists a parser that can accept the given input kind.
    *
    * @param name The name of the input to check.
    * @param kind The kind of the input to check.
    * @return Some(parser) with a parser accepting "kind", or None if one was not found.
    */
  private def checkParserKind(name: String, kind: String): Option[ScriptParser] = {
    val maybe = parsers.values.find(_.acceptsKind(kind))
    if (maybe.isEmpty) logger.warn(s"Skipping input kind with no parser: '$kind' ($name)")
    maybe
  }
}
