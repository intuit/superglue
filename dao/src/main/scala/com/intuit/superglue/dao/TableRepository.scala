package com.intuit.superglue.dao

import com.intuit.superglue.dao.model._

import scala.concurrent.Future

/**
  * Provides access to persisted [[TableEntity]]s.
  *
  * In a graph database, [[TableEntity]]s could be represented as nodes.
  * In a relational database, [[TableEntity]]s could be represented as
  * data in rows of a table.
  */
trait TableRepository {

  /**
    * Queries all table entities that have been stored.
    */
  def getAll: Future[Seq[TableEntity]]

  /**
    * Queries all table entities matching the given name.
    *
    * @param name The name of table entities to search for.
    */
  def getByName(name: String): Future[Seq[TableEntity]]

  /**
    * Inserts a given table entity into storage.
    *
    * @param table The table entity to insert into storage.
    * @return The number of successful inserts (0 or 1).
    */
  def add(table: TableEntity): Future[Int]

  /**
    * Inserts all of the given table entities into storage.
    *
    * @param tables The table entities to insert into storage.
    * @return The number of successful inserts.
    */
  def addAll(tables: Set[TableEntity]): Future[Int]
}
