package com.intuit.superglue.dao

import com.intuit.superglue.dao.model._

import scala.concurrent.Future

/**
  * Provides access to the relationship between scripts and tables.
  *
  * In a graph database, this relationship could be represented as an edge
  * between a Script node and a Table node. In a relational database, this
  * could be a join table which relates Script entities and Table entities.
  */
trait ScriptTableRepository {

  /**
    * Queries all relations between scripts and tables.
    *
    * This only returns rows from the relational table itself, i.e. each element is simply
    * the script ID, table ID, and the direction of the relationship between them.
    *
    * @return
    */
  def getAll: Future[Seq[ScriptTableRelation]]

  /**
    * Queries all related scripts and table entities and returns their joined projection.
    *
    * @return The inner join of related scripts and tables.
    */
  def getAllJoined: Future[Seq[ScriptTableJoin]]

  /**
    * Inserts each ScriptEntity and all of the nested TableEntities within, and creates
    * a relation between each TableEntity and its parent ScriptEntity.
    *
    * @param scripts
    * @return
    */
  def addScriptsWithTables(scripts: Set[ScriptEntity]): Future[(Int, Int, Int)]
  def addScriptWithTables(script: ScriptEntity): Future[(Int, Int, Int)] =
    addScriptsWithTables(Set(script))

  /**
    * Creates a relationship between existing script and table entities.
    *
    * The given[[ScriptEntity]]s and [[TableEntity]]s must be "inserted", meaning that their
    * "id" field must be populated with Some key given by a database insert. If an "uninitialized"
    * entity is given (i.e. one with an "id" field of None), this will return a Failure.
    *
    * A relationship between a Script and a Table can be thought of as a directional edge in a graph.
    * We indicate the direction of the edge as being either an "Input" to or an "Output" from a
    * ScriptEntity.
    *
    * @param elements A collection of relationships to build. These relationships are modeled as a tuple:
    *                 One script, one direction, and one or many tables.
    *                 For each of these elements, a relationship of the given direction is created
    *                 between the single given script and the (possibly one or many) tables.
    * @return The number of entities successfully inserted.
    */
  def linkScriptsAndTables(elements: Seq[(ScriptEntity, Direction, Seq[TableEntity])]): Future[Int]
  def linkScriptsAndTables(script: ScriptEntity, direction: Direction, table: TableEntity): Future[Int] =
    this.linkScriptsAndTables(Seq((script, direction, Seq(table))))
}
