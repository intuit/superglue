package com.intuit.superglue.pipeline.parsers

import com.intuit.superglue.pipeline.Metadata.ScriptMetadata
import com.intuit.superglue.pipeline.producers.ScriptInput

import scala.concurrent.Future

trait ScriptParser {
  def acceptsKind(kind: String): Boolean
  def parse(input: ScriptInput): Future[ScriptMetadata]
  def parserName: String
}
