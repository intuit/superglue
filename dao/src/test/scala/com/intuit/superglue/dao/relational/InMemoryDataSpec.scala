package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao._
import com.intuit.superglue.dao.{ScriptTableRepository, StatementRepository}
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import pureconfig.generic.auto._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.Random

trait InMemoryDataSpec extends FlatSpec with BeforeAndAfterEach {

  private def inMemoryDbConfig(dbName: String): TypesafeConfig = ConfigFactory.parseString(
    s"""
      |profile = "slick.jdbc.H2Profile$$"
      |dataSourceClass = "slick.jdbc.DatabaseUrlDataSource"
      |numThreads = 1
      |db {
      |  driver = "org.h2.Driver"
      |  url = "jdbc:h2:mem:$dbName"
      |  user = ""
      |  password = ""
      |}
    """.stripMargin
  )

  /**
    * A Fixture is a test setup with fresh values. This fixture initializes a new database
    * with a random name and constructs a SuperglueRepository attached to that database.
    * This is in order to prevent concurrent tests from modifying the same tables.
    */
  class Fixture private {
    private val dbName: String = Random.alphanumeric.take(10).mkString
    private val config: TypesafeConfig = inMemoryDbConfig(dbName)
    private val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("", config)
    import dbConfig.profile.api._
    private val db = dbConfig.db

    val sgRepo = new SuperglueRelationalRepository(dbConfig)
    val sgDao: sgRepo.DataLayer = sgRepo.dataLayer
    val tableRepo: TableRepository = sgRepo.tableRepository
    val scriptRepo: ScriptRepository = sgRepo.scriptRepository
    val statementRepo: StatementRepository = sgRepo.statementRepository
    val scriptTableRepo: ScriptTableRepository = sgRepo.scriptTableRepository
    val statementTableRepo: StatementTableRepository = sgRepo.statementTableRepository

    private val schema =
      sgDao.tablesQuery.schema ++
      sgDao.scriptsQuery.schema ++
      sgDao.statementsQuery.schema ++
      sgDao.scriptTableQuery.schema ++
      sgDao.statementTableQuery.schema

    private def initDb(): Unit = {
      Await.result(sgRepo.initialize(testMode = true), 1 second)
    }

    private def dropDb(): Unit = {
      Await.result(db.run(schema.dropIfExists), 1 second)
    }
  }

  object Fixture {
    /**
      * Creates a scope in which the test Fixture is available. After the fixture is done
      * being used, it cleans up the database resources it allocated.
      *
      * Usage:
      * {{{
      *   Fixture { f =>
      *     val tableRepository = f.sgRepo.tableRepository
      *     ...
      *   }
      * }}}
      */
    def apply(f: Fixture => Unit): Unit = {
      val fixture = new Fixture()
      fixture.initDb()
      f(fixture)
      fixture.dropDb()
    }
  }

}
