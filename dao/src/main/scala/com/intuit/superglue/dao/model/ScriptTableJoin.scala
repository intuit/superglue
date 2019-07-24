package com.intuit.superglue.dao.model

import com.intuit.superglue.dao.model.PrimaryKeys._

case class ScriptTableJoin(
  scriptId: ScriptPK,
  tableId: TablePK,
  direction: Direction,
  scriptName: String,
  scriptType: String,
  scriptGitUrl: String,
  scriptHash: String,
  scriptVersionId: Int,
  tableName: String,
  tableSchema: String,
  tablePlatform: String,
)
