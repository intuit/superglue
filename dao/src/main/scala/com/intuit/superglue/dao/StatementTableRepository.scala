package com.intuit.superglue.dao

import com.intuit.superglue.dao.model._

import scala.concurrent.Future

/**
  * Provides access to the relationships between [[StatementEntity]]s
  * and [[TableEntity]]s.
  *
  * In a graph database, a [[StatementTableRelation]] could be represented
  * as an edge between [[StatementEntity]]s and [[TableEntity]].
  * In a relational database, [[StatementTableRelation]]s are represented
  * as a join-table with the IDs of both.
  */
trait StatementTableRepository {

  /**
    * Queries all [[StatementTableRelation]]s that have been stored.
    */
  def getAll: Future[Seq[StatementTableRelation]]

  /**
    * Queries a joined view of all [[StatementTableRelation]]s as [[StatementTableJoin]]s.
    */
  def getAllJoined: Future[Seq[StatementTableJoin]]

  /**
    * Inserts a collection of [[StatementEntity]]s into storage, along with all
    * [[TableEntity]]s they are related to and the relations themselves.
    *
    * In memory, [[StatementEntity]]s "own" the [[TableEntity]]s they refer to.
    * That is, each [[StatementEntity]] object has a list of the [[TableEntity]]
    * objects that it relates to.
    *
    * @param statements The statement entities to persist.
    * @return A tuple of (The number of statements inserted, the number of tables inserted,
    *         the number of relationships created between statements and tables)
    */
  def addStatementsWithTables(statements: Set[StatementEntity]): Future[(Int, Int, Int)]
  def addStatementWithTables(statement: StatementEntity): Future[(Int, Int, Int)] =
    addStatementsWithTables(Set(statement))
}
