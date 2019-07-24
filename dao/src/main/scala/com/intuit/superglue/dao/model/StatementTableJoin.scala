package com.intuit.superglue.dao.model

import com.intuit.superglue.dao.model.PrimaryKeys._

case class StatementTableJoin(
  statementId: StatementPK,
  tableId: TablePK,
  direction: Direction,
  statementType: String,
  statementText: String,
  statementScript: ScriptPK,
  statementVersionId: Int,
  tableName: String,
  tableSchema: String,
  tablePlatform: String,
)
