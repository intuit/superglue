package com.intuit.superglue.pipeline.parsers

import com.intuit.superglue.pipeline.Metadata.StatementMetadataFragment

object NopStatementParser extends StatementParser {
  override def parseStatement(statement: String): StatementMetadataFragment = {
    StatementMetadataFragment(
      statementParser = getClass.getName,
      statementType = "",
      inputObjects = List.empty[String],
      outputObjects = List.empty[String],
      List.empty[Throwable],
    )
  }
}
