package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao._
import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys.TablePK
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  * An implementation of [[SuperglueRepository]] which stores data in a relational database.
  *
  * @param dbConfig A Slick database configuration with settings and credentials
  *                 for connecting to a relational database.
  */
class SuperglueRelationalRepository(dbConfig: DatabaseConfig[JdbcProfile]) extends SuperglueRepository {
  import dbConfig.profile.api._
  private lazy val db = dbConfig.db

  /**
    * Mixes together the interfaces of all the data-access-objects and provides them
    * with the JDBC profile information necessary to communicate with the configured
    * database.
    *
    * @param profile The JDBC profile of the connected database. This ensures that slick
    *                generates SQL statements of the correct dialect for the database
    *                being used.
    */
  private[relational] class DataLayer(val profile: JdbcProfile) extends Profile
    with TableDao
    with ScriptDao
    with StatementDao
    with ScriptTableRelationship
    with StatementTableRelationship
    with LineageViewDao

  /**
    * The instance of the data layer, built using the JDBC profile of the connected db.
    */
  private[relational] lazy val dataLayer = new DataLayer(dbConfig.profile)

  /**
    * Initializes the connected database by creating the necessary tables.
    *
    * Slick currently has a bug where a {{{createIfNotExists}}} statement can
    * still fail if the table being created also specifies an index. If the table
    * and index do exist, {{{createIfNotExists}}} should be a no-op, but an
    * exception is thrown because the index already exists. To work around this,
    * we use the "testMode" flag to only create an index in unit tests, when we
    * know we have a fresh database instance with no existing index.
    *
    * @param testMode Indicates whether this call is being made in a unit test.
    *                 This is a workaround for the bug described above.
    * @return
    */
  override def initialize(testMode: Boolean = false): Future[Unit] = {
    val schema =
      dataLayer.tablesQuery.schema ++
      dataLayer.scriptsQuery.schema ++
      dataLayer.statementsQuery.schema ++
      dataLayer.scriptTableQuery.schema ++
      dataLayer.statementTableQuery.schema

    // This testMode variable is a workaround because our H2 test db
    // fails on createLineageViewSchema for some reason.
    val action = if (testMode) {
      schema.createIfNotExists
    }
    else {
      for {
        _ <- schema.createIfNotExists
        _ <- dataLayer.createLineageViewSchema
      } yield ()
    }
    db.run(action)
  }

  /**
    * The relational implementation of TableRepository.
    */
  override val tableRepository: TableRepository = new TableRepository {
    override def getAll: Future[Seq[TableEntity]] = {
      db.run(dataLayer.getAllTablesQuery.result)
    }

    override def getByName(name: String): Future[Seq[TableEntity]] = {
      db.run(dataLayer.getTablesByNameQuery(name.toUpperCase).result)
    }

    override def add(table: TableEntity): Future[Int] = {
      db.run(dataLayer.upsertTableQuery(table))
    }

    override def addAll(tables: Set[TableEntity]): Future[Int] = {
      db.run(dataLayer.upsertAllTablesQuery(tables)).map(_.sum)
    }
  }

  /**
    * The relational implementation of ScriptRepository.
    */
  override val scriptRepository: ScriptRepository = new ScriptRepository {
    override def getAll: Future[Seq[ScriptEntity]] = {
      db.run(dataLayer.getAllScriptsQuery.result)
    }

    override def getByName(name: String): Future[Seq[ScriptEntity]] = {
      db.run(dataLayer.getScriptsByNameQuery(name).result)
    }

    override def add(script: ScriptEntity): Future[Int] = {
      db.run(dataLayer.upsertScriptQuery(script))
    }

    def addAll(scripts: Set[ScriptEntity]): Future[Int] = {
      if (scripts.isEmpty) return Future.successful(0)
      db.run(dataLayer.upsertAllScriptsQuery(scripts)).map(_.sum)
    }
  }

  /**
    * The relational implementation of ScriptTableRepository.
    */
  override val scriptTableRepository: ScriptTableRepository = new ScriptTableRepository {
    override def getAll: Future[Seq[ScriptTableRelation]] = {
      db.run(dataLayer.getAllScriptAndTableRelationsQuery.result)
    }

    override def getAllJoined: Future[Seq[ScriptTableJoin]] = {
      db.run(dataLayer.getAllTablesAndScriptsJoinedQuery.result)
        .map(_.map(tuple => (ScriptTableJoin.apply _).tupled(tuple)))
    }

    override def addScriptsWithTables(scripts: Set[ScriptEntity]): Future[(Int, Int, Int)] = {
      val allTables = scripts.flatMap(_.tables).flatMap { case (_, table) => table }

      val action = for {
        scriptCount <- dataLayer.upsertAllScriptsQuery(scripts).map(_.sum)
        tableCount <- dataLayer.upsertAllTablesQuery(allTables).map(_.sum)
        relationCount <- dataLayer.upsertScriptsAndTablesQuery(scripts).map(_.sum)
      } yield (scriptCount, tableCount, relationCount)

      db.run(action)
    }

    override def linkScriptsAndTables(entries: Seq[(ScriptEntity, Direction, Seq[TableEntity])]): Future[Int] = {
      if (entries.isEmpty) return Future.successful(0)
      val deduplicated = entries
        .map { case (script, direction, tables) => (script, direction, tables.toSet) }
        .toSet
      db.run(dataLayer.insertTablesForScriptsQuery(deduplicated)).map(_.sum)
    }
  }

  /**
    * The relational implementation of StatementRepository.
    */
  override val statementRepository: StatementRepository = new StatementRepository {
    override def add(statement: StatementEntity): Future[Int] = {
      db.run(dataLayer.upsertStatementQuery(statement))
    }

    override def addAll(statements: Set[StatementEntity]): Future[Int] = {
      db.run(dataLayer.upsertAllStatementsQuery(statements)).map(_.sum)
    }

    override def getAll: Future[Seq[StatementEntity]] = {
      db.run(dataLayer.getAllStatementsQuery.result)
    }

    override def getByType(statementType: String): Future[Seq[StatementEntity]] = {
      db.run(dataLayer.getStatementsByTypeQuery(statementType).result)
    }
  }

  /**
    * The relational implementation of StatementTableRepository.
    */
  override val statementTableRepository: StatementTableRepository = new StatementTableRepository {
    override def getAll: Future[Seq[StatementTableRelation]] = {
      db.run(dataLayer.getAllStatementAndTableRelationsQuery.result)
    }

    override def getAllJoined: Future[Seq[StatementTableJoin]] = {
      db.run(dataLayer.getAllJoinedStatementsAndTablesQuery.result)
        .map(_.map(tuple => (StatementTableJoin.apply _).tupled(tuple)))
    }

    override def addStatementsWithTables(statements: Set[StatementEntity]): Future[(Int, Int, Int)] = {
      val allTables = statements.flatMap(_.tables).flatMap { case (_, table) => table }

      val action = for {
        statementCount <- dataLayer.upsertAllStatementsQuery(statements).map(_.sum)
        tableCount <- dataLayer.upsertAllTablesQuery(allTables).map(_.sum)
        relationCount <- dataLayer.upsertAllStatementTableRelationsQuery(statements).map(_.sum)
      } yield (statementCount, tableCount, relationCount)

      db.run(action)
    }
  }

  /**
    * The relational implementation of LineageViewRepository.
    */
  override val lineageViewRepository: LineageViewRepository = new LineageViewRepository {
    override def getAll: Future[Seq[LineageView]] = {
      db.run(dataLayer.lineageViewQuery.result)
    }

    override def getByTableIdAndDirection(tables: Set[TablePK], direction: Direction): Future[Seq[LineageView]] = {
      db.run(dataLayer.getLineageByTableIdAndDirection(tables, direction).result)
    }
  }
}
