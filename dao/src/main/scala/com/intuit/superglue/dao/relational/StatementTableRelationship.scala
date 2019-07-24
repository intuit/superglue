package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * When implemented for a type that has a JDBC profile, this trait
  * provides implementations of relational queries for [[StatementTableRelation]]s.
  */
trait StatementTableRelationship { self: Profile with TableDao with StatementDao =>
  import profile.api._
  import com.intuit.superglue.dao.model.Direction._

  /**
    * A mapping from a [[StatementTableRelation]] onto a relational database table.
    */
  class StatementTableDef(tag: Tag) extends Table[StatementTableRelation](tag, "STATEMENT_TABLE_RELATION") {
    def statementId = column[StatementPK]("statement_id")
    def tableId = column[TablePK]("table_id")
    def direction = column[Direction]("direction")
    def relationId = column[StatementTablePK]("relation_id", O.PrimaryKey)

    /**
      * Converts a row from a relational database into a [[StatementTableRelation]].
      */
    def into(tuple: (StatementPK, TablePK, Direction, StatementTablePK)): StatementTableRelation = {
      val (statementId, tableId, direction, relationId) = tuple
      StatementTableRelation(statementId, tableId, direction, relationId)
    }

    /**
      * Converts a [[StatementTableRelation]] into a row for a relational database.
      */
    def from(relation: StatementTableRelation): Option[(StatementPK, TablePK, Direction, StatementTablePK)] =
      Some((relation.statementId,
        relation.tableId,
        relation.direction,
        relation.relationId))

    /**
      * Defines the full projection of the relational database table.
      */
    override def * = (statementId, tableId, direction, relationId) <> (into, from)

    /**
      * Defines a relational database index on the "direction" column.
      */
    def directionIndex = index("statement_table_direction_idx", direction)

    /**
      * Defines a foreign key to the ID of a [[StatementEntity]] stored in another table.
      */
    def statementFk = foreignKey("statement_fk", statementId, statementsQuery)(_.id)

    /**
      * Defines a foreign key to the ID of a [[TableEntity]] stored in another table.
      */
    def tableFk = foreignKey("table_fk", tableId, tablesQuery)(_.id)
  }

  lazy val statementTableQuery = TableQuery[StatementTableDef]

  def getAllStatementAndTableRelationsQuery = statementTableQuery

  def getAllJoinedStatementsAndTablesQuery = statementTableQuery
    .join(tablesQuery).on(_.tableId === _.id)
    .join(statementsQuery).on(_._1.statementId === _.id)
    .map { case ((relationship, table), statement) =>
      (
        statement.id,
        table.id,
        relationship.direction,
        statement.statementType,
        statement.text,
        statement.scriptId,
        statement.versionId,
        table.name,
        table.schema,
        table.platform,
      )
    }

  def upsertStatementTableRelationQuery(relation: StatementTableRelation) = {
    statementTableQuery insertOrUpdate relation
  }

  def upsertAllStatementTableRelationsQuery(statements: Set[StatementEntity]) = {
    val relations = for {
      statementEntity <- statements
      (direction, tables) <- statementEntity.tables
      table <- tables
    } yield StatementTableRelation(statementEntity.id, table.id, direction)

    DBIO.sequence(relations.toSeq.map(upsertStatementTableRelationQuery))
  }
}
