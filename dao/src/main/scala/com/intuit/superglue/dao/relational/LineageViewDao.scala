package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * When implemented for a type that has a JDBC profile, this trait
  * provides implementations of relational queries for [[ScriptTableRelation]]s.
  */
trait LineageViewDao { self: Profile =>
  import profile.api._

  /**
    * A mapping from a [[LineageView]] onto a relational database table.
    */
  class LineageViewDef(tag: Tag) extends Table[LineageView](tag, "LINEAGE_VW") {
    def inputTableId = column[TablePK]("input_table_id")
    def inputTableName = column[String]("input_table_name")
    def outputTableId = column[TablePK]("output_table_id")
    def outputTableName = column[String]("output_table_name")
    def scriptId = column[ScriptPK]("script_id")
    def statementId = column[StatementPK]("statement_id")

    /**
      * Defines the full projection of the relational database table.
      */
    override def * = (inputTableId, inputTableName, outputTableId, outputTableName, scriptId, statementId) <>
      (LineageView.tupled, LineageView.unapply)
  }

  // TableQuery value which represents the mapped underlying table in DB:
  val lineageViewQuery = TableQuery[LineageViewDef]

  def getLineageByTableIdAndDirection(tables: Set[TablePK], direction: Direction) = direction match {
    case Input => lineageViewQuery.filter(_.inputTableId.inSetBind(tables))
    case Output => lineageViewQuery.filter(_.outputTableId.inSetBind(tables))
  }

  val createLineageViewSchema =
    sqlu"""CREATE VIEW LINEAGE_VW AS
           SELECT
             TS.table_id AS input_table_id,
             TS.table_name AS input_table_name,
             OUTPUT_QUERY.table_id AS output_table_id,
             OUTPUT_QUERY.table_name AS output_table_name,
             SS.script_id AS script_id,
             STMT_TABLE.statement_id AS statement_id
           FROM (
             SELECT
               STMT_TABLE.statement_id AS statement_id,
               STMT_TABLE.table_id AS table_id,
               TS.table_name AS table_name,
               SS.statement_script AS statement_script
             FROM TABLES TS
             INNER JOIN STATEMENT_TABLE_RELATION STMT_TABLE ON STMT_TABLE.table_id = TS.table_id
             INNER JOIN STATEMENTS SS ON SS.statement_id = STMT_TABLE.statement_id
             WHERE STMT_TABLE.direction='O'
           ) OUTPUT_QUERY
           INNER JOIN STATEMENT_TABLE_RELATION STMT_TABLE ON STMT_TABLE.statement_id = OUTPUT_QUERY.statement_id
           INNER JOIN TABLES TS ON TS.table_id = STMT_TABLE.table_id
           INNER JOIN SCRIPTS SS ON OUTPUT_QUERY.statement_script = SS.script_id
           WHERE STMT_TABLE.direction='I'
      """

  val dropLineageViewSchema = sqlu"""DROP VIEW LINEAGE_VW"""
}
