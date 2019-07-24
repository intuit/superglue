package com.intuit.superglue.dao

import com.intuit.superglue.dao.model._

import scala.concurrent.Future

/**
  * Provides access to persisted [[StatementEntity]]s.
  */
trait StatementRepository {

  /**
    * Queries all [[StatementEntity]]s that have been stored.
    */
  def getAll: Future[Seq[StatementEntity]]

  /**
    * Queries all [[StatementEntity]]s that have the given type.
    *
    * In SQL, the "type" refers to the type of statement that's represented,
    * e.g. "SELECT", "INSERT", etc.
    *
    * @param typ The type of statement to query.
    * @return A collection of [[StatementEntity]]s with the given type.
    */
  def getByType(typ: String): Future[Seq[StatementEntity]]

  /**
    * Inserts the given [[StatementEntity]] into storage.
    *
    * @param statement The statement entity to store.
    * @return The number of successful inserts (0 or 1).
    */
  def add(statement: StatementEntity): Future[Int]

  /**
    * Inserts all of the given [[StatementEntity]]s into storage.
    *
    * @param statements The statement entities to store.
    * @return The number of successful inserts.
    */
  def addAll(statements: Set[StatementEntity]): Future[Int]
}
