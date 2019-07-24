package com.intuit.superglue.pipeline.parsers

import com.typesafe.config.{Config => TypesafeConfig}

class SqlScriptParser(
  override val preparsers: List[Preprocessor] = List(NopPreprocessor),
  override val splitter: StatementSplitter = SimpleStatementSplitter,
  override val statementParser: StatementParser = new CalciteStatementParser(),
)(implicit rootConfig: TypesafeConfig) extends StagedScriptParser(
  preparsers,
  splitter,
  statementParser,
) {
  override def parserName: String = "sql"
}
