package com.intuit.superglue.dao.model

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.intuit.superglue.dao.model.PrimaryKeys._

case class StatementEntity(
  statementType: String,
  text: String,
  scriptId: ScriptPK,
  versionId: Int,
  id: StatementPK,
  tables: Set[(Direction, Set[TableEntity])],
)

object StatementEntity {
  def apply(
    statementType: String,
    text: String,
    scriptId: ScriptPK,
    versionId: Int = 0,
    tables: Set[(Direction, Set[TableEntity])] = Set.empty,
  ): StatementEntity = {
    val normalizedType = statementType.toUpperCase

    val id = Hashing.sipHash24().newHasher()
      .putString(statementType, Charsets.UTF_8)
      .putString(text, Charsets.UTF_8)
      .putLong(scriptId.value)
      .hash().asLong()
    StatementEntity(normalizedType, text, scriptId, versionId, StatementPK(id), tables)
  }
}
