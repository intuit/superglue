package com.intuit.superglue.dao

import com.intuit.superglue.dao.model._

import scala.concurrent.Future

/**
  * Provides access to persisted [[ScriptEntity]]s.
  *
  * In a graph database, [[ScriptEntity]]s could be represented as nodes.
  * In a relational database, [[ScriptEntity]]s could be represented as rows in a table.
  */
trait ScriptRepository {

  /**
    * Queries all script entities that have been stored.
    */
  def getAll: Future[Seq[ScriptEntity]]

  /**
    * Queries all scripts matching the given name.
    *
    * @param name The name of script entities to search for.
    */
  def getByName(name: String): Future[Seq[ScriptEntity]]

  /**
    * Inserts a given script entity into storage.
    *
    * This method only persists the ScriptEntity itself. If this ScriptEntity has
    * tables in its `tables` field, this method will not insert those tables.
    * For that, see [[ScriptTableRepository]].
    *
    * @param script The script entity to insert into storage.
    * @return The number of successful inserts (0 or 1).
    */
  def add(script: ScriptEntity): Future[Int]

  /**
    * Inserts all of the given script entities into storage.
    *
    * This method only persists the ScriptEntity objects. If any ScriptEntity has
    * tables in its `tables` field, this method will not insert those tables.
    * For that, see [[ScriptTableRepository]].
    *
    * @param scripts The script entities to insert into storage.
    * @return The number of successful inserts.
    */
  def addAll(scripts: Set[ScriptEntity]): Future[Int]
}
