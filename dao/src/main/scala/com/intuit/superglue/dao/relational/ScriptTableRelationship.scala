package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * When implemented for a type that has a JDBC profile, this trait
  * provides implementations of relational queries for [[ScriptTableRelation]]s.
  */
trait ScriptTableRelationship { self: Profile with TableDao with ScriptDao =>
  import profile.api._
  import com.intuit.superglue.dao.model.Direction._

  /**
    * A mapping from a [[ScriptTableRelation]] onto a relational database table.
    */
  class ScriptTableDef(tag: Tag) extends Table[ScriptTableRelation](tag, "SCRIPT_TABLE_RELATION") {
    def scriptId = column[ScriptPK]("script_id")
    def tableId = column[TablePK]("table_id")
    def direction = column[Direction]("direction")
    def relationId = column[ScriptTablePK]("relation_id", O.PrimaryKey)

    /**
      * Converts a row from a relational database into a [[ScriptTableRelation]].
      */
    def into(tuple: (ScriptPK, TablePK, Direction, ScriptTablePK)): ScriptTableRelation = {
      val (scriptId, tableId, direction, relationId) = tuple
      ScriptTableRelation(scriptId, tableId, direction, relationId)
    }

    /**
      * Converts a [[ScriptTableRelation]] into a row for a relational database.
      */
    def from(scriptTableRelation: ScriptTableRelation): Option[(ScriptPK, TablePK, Direction, ScriptTablePK)] =
      Some((scriptTableRelation.scriptId,
        scriptTableRelation.tableId,
        scriptTableRelation.direction,
        scriptTableRelation.relationId))

    /**
      * Defines the full projection of the relational database table.
      */
    override def * = (scriptId, tableId, direction, relationId) <> (into, from)

    /**
      * Defines a relational database index on the "direction" column.
      */
    def directionIndex = index("script_table_direction_idx", direction)

    /**
      * Defines a foreign key to the ID of a [[ScriptEntity]] stored in another table.
      */
    def scriptFk = foreignKey("script_fk", scriptId, scriptsQuery)(_.id)

    /**
      * Defines a foreign key to the ID of a [[TableEntity]] stored in another table.
      */
    def tableFk = foreignKey("table_fk", tableId, tablesQuery)(_.id)
  }

  lazy val scriptTableQuery = TableQuery[ScriptTableDef]

  def getAllScriptAndTableRelationsQuery = scriptTableQuery

  def getAllTablesAndScriptsJoinedQuery = scriptTableQuery
    .join(tablesQuery).on(_.tableId === _.id)
    .join(scriptsQuery).on(_._1.scriptId === _.id)
    .map { case ((relationship, table), script) =>
      (
        script.id,
        table.id,
        relationship.direction,
        script.name,
        script.scriptType,
        script.scriptGitUrl,
        script.scriptHash,
        script.scriptVersionId,
        table.name,
        table.schema,
        table.platform,
      )
    }

  def upsertScriptTableRelationQuery(relation: ScriptTableRelation) = {
    scriptTableQuery insertOrUpdate relation
  }

  def insertTablesForScriptsQuery(entries: Set[(ScriptEntity, Direction, Set[TableEntity])]) = {
    val relations = entries.flatMap { case (script, direction, theTables) =>
      theTables.map(table => ScriptTableRelation(script.id, table.id, direction))
    }

    DBIO.sequence(relations.toSeq.map(upsertScriptTableRelationQuery))
  }

  def upsertScriptsAndTablesQuery(scriptEntities: Set[ScriptEntity]) = {
    val relations = for {
      scriptEntity <- scriptEntities
      (direction, tables) <- scriptEntity.tables
      table <- tables
    } yield ScriptTableRelation(scriptEntity.id, table.id, direction)

    DBIO.sequence(relations.toSeq.map(upsertScriptTableRelationQuery))
  }

}
