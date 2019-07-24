package com.intuit.superglue.dao

import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys._

import scala.concurrent.Future

/**
  * Provides access to a view of table lineage.
  *
  * In a graph database, this would simply represent a query
  * on the edge between two tables.
  * In a relational database, this is represented by joining
  * the [[StatementTableRelation]] with itself to produce an
  * input/output table pair.
  */
trait LineageViewRepository {

  /**
    * Queries all of the views of table lineage.
    */
  def getAll: Future[Seq[LineageView]]

  /**
    * Queries table lineage views based on the IDs of tables
    * and the direction edge extending from them (i.e. Input or Output).
    *
    * @param tables The IDs of tables to fetch lineage of.
    * @param direction
    * @return
    */
  def getByTableIdAndDirection(tables: Set[TablePK], direction: Direction): Future[Seq[LineageView]]
}
