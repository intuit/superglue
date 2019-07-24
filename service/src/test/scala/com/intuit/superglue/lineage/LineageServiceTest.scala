package com.intuit.superglue.lineage

import com.intuit.superglue.dao._
import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys._
import com.intuit.superglue.lineage.model.Link
import com.intuit.superglue.lineage.model.Node.TableNode
import org.scalatest.FlatSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class LineageServiceTest extends FlatSpec {

  private def makeRepository(views: Set[LineageView]) = new SuperglueRepository {
    override def initialize(testMode: Boolean): Future[Unit] = Future.successful(())
    override val tableRepository: TableRepository = new TableRepository {
      override def getAll: Future[Seq[TableEntity]] = Future.successful(
        views.flatMap(view => Set(
          TableEntity(view.inputTableName, "", "", view.inputTableId),
          TableEntity(view.outputTableName, "", "", view.outputTableId),
        )).toSeq
      )
      override def getByName(name: String): Future[Seq[TableEntity]] = getAll.map(_.filter(_.name == name))
      override def add(table: TableEntity): Future[Int] = null
      override def addAll(tables: Set[TableEntity]): Future[Int] = null
    }
    override val scriptRepository: ScriptRepository = null
    override val statementRepository: StatementRepository = null
    override val scriptTableRepository: ScriptTableRepository = null
    override val statementTableRepository: StatementTableRepository = null
    override val lineageViewRepository: LineageViewRepository = new LineageViewRepository {
      override def getAll: Future[Seq[LineageView]] = Future.successful(views.toSeq)
      override def getByTableIdAndDirection(tables: Set[PrimaryKeys.TablePK], direction: Direction): Future[Seq[LineageView]] = {
        val collection = direction match {
          case Input => views.filter(view => tables.contains(view.inputTableId))
          case Output => views.filter(view => tables.contains(view.outputTableId))
        }
        Future.successful(collection.toSeq)
      }
    }
  }

  "The LineageService" should "collect full forward lineage" in {
    val views = (-100 to 100)
      .map(num => LineageView(TablePK(num), "", TablePK(num + 1), "", ScriptPK(0), StatementPK(0)))
      .map {
        case LineageView(pk @ TablePK(0), _, a, b, c, d) => LineageView(pk, "START", a, b, c, d)
        case LineageView(a, b, pk @ TablePK(0), _, c, d) => LineageView(a, b, pk, "START", c, d)
        case other => other
      }.toSet
    val repository = makeRepository(views)
    val lineageService = new LineageService(repository)
    val futureLineageGraph = lineageService.tableLineage("START", Some(0), None)
    val lineageGraph = Await.result(futureLineageGraph, 1 second)

    val expectedLinks = (0 to 100)
        .map(num => Link(TableNode(TablePK(num), ""), TableNode(TablePK(num + 1), "")))
        .map {
          // The table name for id 0 should be "START"
          case Link(TableNode(pk @ TablePK(0), _, _), out) => Link(TableNode(pk, "START"), out)
          case Link(in, TableNode(pk @ TablePK(0), _, _)) => Link(in, TableNode(pk, "START"))
          case other => other
        }.toSet

    assert(expectedLinks == lineageGraph.links)
  }

  it should "collect full backward lineage" in {
    val views = (-100 to 100)
      .map(num => LineageView(TablePK(num), "", TablePK(num + 1), "", ScriptPK(0), StatementPK(0)))
      .map {
        case LineageView(pk @ TablePK(0), _, a, b, c, d) => LineageView(pk, "START", a, b, c, d)
        case LineageView(a, b, pk @ TablePK(0), _, c, d) => LineageView(a, b, pk, "START", c, d)
        case other => other
      }.toSet
    val repository = makeRepository(views)
    val lineageService = new LineageService(repository)
    val futureLineageGraph = lineageService.tableLineage("START", None, Some(0))
    val lineageGraph = Await.result(futureLineageGraph, 1 second)

    val expectedLinks = (-100 until 0)
      .map(num => Link(TableNode(TablePK(num), ""), TableNode(TablePK(num + 1), "")))
      .map {
        // The table name for id 0 should be "START"
        case Link(TableNode(pk @ TablePK(0), _, _), out) => Link(TableNode(pk, "START"), out)
        case Link(in, TableNode(pk @ TablePK(0), _, _)) => Link(in, TableNode(pk, "START"))
        case other => other
      }.toSet

    assert(expectedLinks == lineageGraph.links)
  }

  it should "collect full backward and forward lineage" in {
    val views = (-50 to 50)
      .map(num => LineageView(TablePK(num), "", TablePK(num + 1), "", ScriptPK(0), StatementPK(0)))
      .map {
        case LineageView(pk @ TablePK(0), _, a, b, c, d) => LineageView(pk, "START", a, b, c, d)
        case LineageView(a, b, pk @ TablePK(0), _, c, d) => LineageView(a, b, pk, "START", c, d)
        case other => other
      }.toSet
    val repository = makeRepository(views)
    val lineageService = new LineageService(repository)
    val futureLineageGraph = lineageService.tableLineage("START", None, None)
    val lineageGraph = Await.result(futureLineageGraph, 1 second)

    val expectedLinks = (-50 to 50)
      .map(num => Link(TableNode(TablePK(num), ""), TableNode(TablePK(num + 1), "")))
      .map {
        // The table name for id 0 should be "START"
        case Link(TableNode(pk @ TablePK(0), _, _), out) => Link(TableNode(pk, "START"), out)
        case Link(in, TableNode(pk @ TablePK(0), _, _)) => Link(in, TableNode(pk, "START"))
        case other => other
      }.toSet

    assert(expectedLinks == lineageGraph.links)
  }

  it should "collect bounded backward and forward lineage" in {
    val views = (-100 to 100)
      .map(num => LineageView(TablePK(num), "", TablePK(num + 1), "", ScriptPK(0), StatementPK(0)))
      .map {
        case LineageView(pk @ TablePK(0), _, a, b, c, d) => LineageView(pk, "START", a, b, c, d)
        case LineageView(a, b, pk @ TablePK(0), _, c, d) => LineageView(a, b, pk, "START", c, d)
        case other => other
      }.toSet
    val repository = makeRepository(views)
    val lineageService = new LineageService(repository)
    val futureLineageGraph = lineageService.tableLineage("START", Some(25), Some(50))
    val lineageGraph = Await.result(futureLineageGraph, 1 second)

    val expectedLinks = (-25 until 50)
      .map(num => Link(TableNode(TablePK(num), ""), TableNode(TablePK(num + 1), "")))
      .map {
        // The table name for id 0 should be "START"
        case Link(TableNode(pk @ TablePK(0), _, _), out) => Link(TableNode(pk, "START"), out)
        case Link(in, TableNode(pk @ TablePK(0), _, _)) => Link(in, TableNode(pk, "START"))
        case other => other
      }.toSet

    assert(expectedLinks == lineageGraph.links)
  }

  it should "not loop infinitely on a cycle" in {
    val views = (-100 to 100)
      .map(num => LineageView(TablePK(num), "", TablePK(num + 1), "", ScriptPK(0), StatementPK(0)))
      .map {
        case LineageView(pk @ TablePK(0), _, a, b, c, d) => LineageView(pk, "START", a, b, c, d)
        case LineageView(a, b, pk @ TablePK(0), _, c, d) => LineageView(a, b, pk, "START", c, d)
        case other => other
      }
    val cycleViews = views :+ LineageView(TablePK(99), "", TablePK(-100), "", ScriptPK(0), StatementPK(0))
    val repository = makeRepository(cycleViews.toSet)
    val lineageService = new LineageService(repository)
    val futureLineageGraph = lineageService.tableLineage("START", None, None)
    Await.result(futureLineageGraph, 1 second) // This will fail if we loop infinitely
  }
}
