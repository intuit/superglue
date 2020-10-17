package com.intuit.superglue.pipeline.parsers

import com.intuit.superglue.pipeline.Metadata.StatementMetadataFragment

/**
  * A StatementParser defines how to read a single statement and extract
  * some metadata from it.
  */
trait StatementParser {
  def parseStatement(statement: String, dialect: Option[String] = None): StatementMetadataFragment
}
