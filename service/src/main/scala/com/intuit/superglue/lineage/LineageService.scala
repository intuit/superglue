package com.intuit.superglue.lineage

import scala.language.postfixOps
import collection.JavaConverters._
import com.intuit.superglue.dao.SuperglueRepository
import com.intuit.superglue.dao.model.PrimaryKeys.TablePK
import com.intuit.superglue.dao.model.{Direction, Input, LineageView, Output}
import com.intuit.superglue.lineage.model.Node.TableNode
import com.intuit.superglue.lineage.model.{Graph, Link, Node}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Provides an API for retrieving data lineage as a graph.
  */
class LineageService(val repository: SuperglueRepository) {

  private val lineageCacheService = new LineageCacheService(repository)

  /**
    * Lineage extends forward and backward to a certain depth, where the
    * depth represents the number of hops to search in that direction.
    *
    * @param tableName The name of the table to fetch lineage for. *
    * @param backwardDepth Some(depth) to search "depth" hops backward, or None to do a full search.
    * @param forwardDepth Some(depth) to search "depth" hops forward, or None to do a full search.
    */
  def tableLineage(tableName: String, backwardDepth: Option[Int], forwardDepth: Option[Int]): Future[Graph] = {
    (backwardDepth, forwardDepth) match {
      case (Some(bw), _) if bw < 0 => return Future.failed(new Exception("Backward depth cannot be negative"))
      case (_, Some(fw)) if fw < 0 => return Future.failed(new Exception("Forward depth cannot be negative"))
      case _ =>
    }

    if (backwardDepth.contains(0) && forwardDepth.contains(0)) {
      // return selected table with 0 depth
      val singleNode = for {
        table <- repository.tableRepository.getByName(tableName)
        tableHead = table.head
      } yield TableNode(tableHead.id, tableHead.name)
      singleNode.map { case TableNode(pk, name, _) =>
        Graph(Set(TableNode(pk, name, "selected")), Set.empty[Link])
      }
    }
    else {
      val futureGraph = for {
        table <- repository.tableRepository.getByName(tableName)
        tablePK = table.head.id
      } yield tableLineage(Set(tablePK), Set(tablePK), Graph.empty, backwardDepth, forwardDepth)
      futureGraph.flatten.map { case Graph(nodes, links) =>
        val annotatedNodes = nodes.map {
          case TableNode(pk, name, _) if name.toLowerCase == tableName.toLowerCase => TableNode(pk, name, "selected")
          case other => other
        }
        Graph(annotatedNodes, links)
      }
    }
  }

  private def tableLineage(
    backwardFringe: Set[TablePK],
    forwardFringe: Set[TablePK],
    traversed: Graph,
    backwardDepth: Option[Int],
    forwardDepth: Option[Int],
    backwardVisited: Set[TablePK] = Set.empty,
    forwardVisited: Set[TablePK]= Set.empty,
  ): Future[Graph] = {
    if (backwardFringe.isEmpty && forwardFringe.isEmpty) return Future.successful(traversed)
    if (backwardDepth.contains(0) && forwardDepth.contains(0)) return Future.successful(traversed)

    // Don't traverse backward if the backward depth is zero
    val backwardLineageViews = if (backwardDepth.contains(0)) Future.successful(Set.empty) else {
      // Gets the backward lineage for a given set of tables by querying the cache
      getLineageViewResultSet(backwardFringe, Output)
        .map(_.filter(view => !backwardVisited.contains(view.outputTableId)))
    }

    // Don't traverse forward if the forward depth is zero
    val forwardLineageViews = if (forwardDepth.contains(0)) Future.successful(Set.empty) else {
      // Gets the forward lineage for a given set of tables by querying the cache
      getLineageViewResultSet(forwardFringe, Input)
        .map(_.filter(view => !forwardVisited.contains(view.inputTableId)))
    }

    // Decrement the traversal depths
    val (bwDepth, fwDepth) = (backwardDepth, forwardDepth) match {
      case (Some(bw), Some(fw)) if bw > 0 && fw > 0 => (Some(bw - 1), Some(fw - 1))
      case (Some(bw), fw) if bw > 0 => (Some(bw - 1), fw)
      case (bw, Some(fw)) if fw > 0 => (bw, Some(fw - 1))
      case depth => depth
    }

    val bwVisited = backwardVisited ++ backwardFringe
    val fwVisited = forwardVisited ++ forwardFringe

    val futureGraph = for {
      fwLineage <- forwardLineageViews
      bwLineage <- backwardLineageViews
      nextFw = fwLineage.map(_.outputTableId)
      nextBw = bwLineage.map(_.inputTableId)
      graph = constructNodesAndLinks(fwLineage ++ bwLineage)
    } yield tableLineage(
      nextBw, nextFw,
      Graph(traversed.nodes ++ graph.nodes, traversed.links ++ graph.links),
      bwDepth, fwDepth,
      bwVisited, fwVisited,
    )
    futureGraph.flatten
  }

  private def getLineageViewResultSet(tableNames: Set[TablePK], direction: Direction): Future[Set[LineageView]] = {

    // get resultset from cache
    val cachedLineageView: Set[LineageView] = tableNames.flatMap { tableName =>
      lineageCacheService.getCachedLineageView(tableName, direction).getOrElse(Set.empty[LineageView])
    }

    // find tables not in cache
    val tablesNotInCache: Set[TablePK] = direction match {
      case Output => tableNames -- cachedLineageView.map(_.outputTableId)
      case Input => tableNames -- cachedLineageView.map(_.inputTableId)
    }

    val futureDBResultSet = if (tablesNotInCache.isEmpty) Future.successful(Set.empty[LineageView]) else {
      // fetch tables not in cache from DB
      repository.lineageViewRepository.getByTableIdAndDirection(tablesNotInCache, direction).map(_.toSet).map { value =>
        // add to cache
        lineageCacheService.addLineageViewsToCache(tablesNotInCache, direction, value)
        value
      }
    }

    futureDBResultSet.map(_ ++ cachedLineageView)
  }

  /** This method constructs lineage for a a given depth level.
    *
    * For Example for Set[LineageView] - [{"outputTableId":Table1,"outputTableName":"Table1","inputTableId":Table2,"inputTableName","Table2"},
    * {"outputTableId":Table1,"outputTableName":"Table1","inputTableId":Table3,"inputTableName","Table3"},
    * {"outputTableId":Table0,"outputTableName":"Table0","inputTableId":Table1,"inputTableName","Table1"}]
    *
    * Lineage for Table 1 will be : Table2->Table1->Table0
    * Table3->
    *
    * @param tables Set of tables for which we want to construct Lineage
    * @return Lineage for the given depth level
    */
  private def constructNodesAndLinks(tables: Set[LineageView]): Graph = {

    // Seq[LineageView] is iterated in parallel so using concurrent collections
    // https://www.scala-lang.org/docu/files/collections-api/collections_11.html recommends to use java concurrent collection
    val mNodes = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap[Node, java.lang.Boolean]).asScala
    val mLinks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap[Link, java.lang.Boolean]).asScala

    tables.par.foreach { case LineageView(inputTableId, inputTableName, outputTableId, outputTableName, _, _) =>
      val inputTable: Node = TableNode(inputTableId, inputTableName)
      val outputTable: Node = TableNode(outputTableId, outputTableName)

      mNodes += inputTable
      mNodes += outputTable

      val link = Link(inputTable, outputTable)
      mLinks += link
    }

    Graph(mNodes.toSet, mLinks.toSet)
  }
}
