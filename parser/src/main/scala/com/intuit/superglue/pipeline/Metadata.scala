package com.intuit.superglue.pipeline

import java.time.LocalDateTime

object Metadata {

  val UNKNOWN_STATEMENT_TYPE = "UNKNOWN"

  case class ScriptMetadata(
    scriptName: String,
    scriptSource: String,
    scriptKind: String,
    scriptDialect: Option[String],
    scriptParser: String,
    scriptParseStartTime: LocalDateTime,
    scriptParseEndTime: LocalDateTime,
    statementsMetadata: List[StatementMetadata],
    errors: List[Throwable],
  )

  case class StatementMetadata(
    statementText: String,
    statementIndex: Int,
    statementParseStartTime: LocalDateTime,
    statementParseEndTime: LocalDateTime,
    statementMetadataFragment: StatementMetadataFragment,
  )

  case class StatementMetadataFragment(
    statementParser: String,
    statementType: String,
    inputObjects: List[String],
    outputObjects: List[String],
    errors: List[Throwable],
  )

  object StatementMetadataFragment {
    def apply(statementParser: String, errors: List[Throwable]): StatementMetadataFragment = {
      StatementMetadataFragment(
        statementParser,
        UNKNOWN_STATEMENT_TYPE,
        List.empty[String],
        List.empty[String],
        errors,
      )
    }
  }
}
