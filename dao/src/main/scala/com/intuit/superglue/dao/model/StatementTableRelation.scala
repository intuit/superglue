package com.intuit.superglue.dao.model

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * Indicates a directed relationship between a Statement and a Table.
  *
  * An instance of this relation can be thought of as a directed edge in a graph,
  * where the Statement and Table are nodes.
  *
  * @param statementId The ID of the statement in this relationship
  * @param tableId The ID of the table in this relationship
  * @param direction The direction of data flow, from the perspective of the statement.
  */
case class StatementTableRelation(
  statementId: StatementPK,
  tableId: TablePK,
  direction: Direction,
  relationId: StatementTablePK,
)

object StatementTableRelation {
  def apply(
    statementId: StatementPK,
    tableId: TablePK,
    direction: Direction,
  ): StatementTableRelation = {
    val id = Hashing.sipHash24().newHasher()
      .putLong(statementId.value)
      .putLong(tableId.value)
      .putString(direction.name, Charsets.UTF_8)
      .hash().asLong()
    new StatementTableRelation(statementId, tableId, direction, StatementTablePK(id))
  }
}
