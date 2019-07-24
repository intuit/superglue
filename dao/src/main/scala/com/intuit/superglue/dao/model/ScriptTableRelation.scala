package com.intuit.superglue.dao.model

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * Indicates a directed relationship between a Script and a Table.
  *
  * An instance of this relation can be thought of as a directed edge in a graph,
  * where the Script and Table are nodes.
  *
  * @param scriptId The ID of the script in this relationship
  * @param tableId The ID of the table in this relationship
  * @param direction The direction of data flow, from the perspective of the script.
  */
case class ScriptTableRelation(
  scriptId: ScriptPK,
  tableId: TablePK,
  direction: Direction,
  relationId: ScriptTablePK,
)

object ScriptTableRelation {
  def apply(
    scriptId: ScriptPK,
    tableId: TablePK,
    direction: Direction,
  ): ScriptTableRelation = {
    val id = Hashing.sipHash24().newHasher()
      .putLong(scriptId.value)
      .putLong(tableId.value)
      .putString(direction.name, Charsets.UTF_8)
      .hash().asLong()
    ScriptTableRelation(scriptId, tableId, direction, ScriptTablePK(id))
  }
}
