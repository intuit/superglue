package com.intuit.superglue.dao.model

import com.intuit.superglue.dao.model.PrimaryKeys._

case class LineageView(
  inputTableId: TablePK,
  inputTableName: String,
  outputTableId: TablePK,
  outputTableName: String,
  scriptId: ScriptPK,
  statementId: StatementPK
)
