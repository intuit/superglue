package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * When implemented for a type that has a JDBC profile, this trait
  * provides implementations of relational queries for [[StatementEntity]]s.
  */
trait StatementDao { self: Profile with ScriptDao =>
  import profile.api._

  /**
    * A mapping from [[StatementEntity]] onto a relational database table.
    */
  class StatementDef(tag: Tag) extends Table[StatementEntity](tag, "STATEMENTS") {
    def statementType = column[String]("statement_type", O.Length(100))
    def text = column[String]("statement_text", O.Length(4000))
    def scriptId = column[ScriptPK]("statement_script")
    def versionId = column[Int]("version_id")
    def id = column[StatementPK]("statement_id", O.PrimaryKey)

    /**
      * Converts a row from a relational database into a [[StatementEntity]].
      */
    def intoStatement(data: (String, String, ScriptPK, Int, StatementPK)): StatementEntity = {
      val (sType, text, scriptId, versionId, id) = data
      StatementEntity(sType, text, scriptId, versionId, id, Set.empty)
    }

    /**
      * Converts a [[StatementEntity]] into a row for a relational database.
      */
    def fromStatement(statementEntity: StatementEntity): Option[(String, String, ScriptPK, Int, StatementPK)] = {
      Some((statementEntity.statementType,
        statementEntity.text,
        statementEntity.scriptId,
        statementEntity.versionId,
        statementEntity.id))
    }

    /**
      * Defines the full projection of the relational database table.
      */
    override def * = (statementType, text, scriptId, versionId, id) <> (intoStatement, fromStatement)

    /**
      * Defines a foreign key to the ID of a [[ScriptEntity]] stored in another table.
      */
    def scriptFk = foreignKey("script_fk", scriptId, scriptsQuery)(_.id)
  }

  lazy val statementsQuery = TableQuery[StatementDef]

  def getAllStatementsQuery = statementsQuery

  def getStatementsByTypeQuery(statementType: String) =
    statementsQuery.filter(_.statementType === statementType)

  def upsertStatementQuery(statement: StatementEntity) =
    statementsQuery insertOrUpdate statement

  def upsertAllStatementsQuery(statements: Set[StatementEntity]) =
    DBIO.sequence(statements.toSeq.map(upsertStatementQuery))
}
