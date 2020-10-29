package com.intuit.superglue.integration

import java.io.PrintStream
import java.nio.file.FileSystem

import com.intuit.superglue.dao.SuperglueRepository
import com.intuit.superglue.pipeline.consumers.DatabaseConsumer
import com.intuit.superglue.pipeline.{FsSpec, ParsingPipeline, ScriptInputSpec, Sink, Source}
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class ParserDaoIntegrationTest extends ScriptInputSpec with FsSpec {

  private def inMemoryDbConfig(dbName: String): TypesafeConfig = ConfigFactory.parseString(
    s"""
       |com.intuit.superglue {
       |  dao {
       |    backend = "relational"
       |    relational {
       |      profile = "slick.jdbc.H2Profile$$"
       |      dataSourceClass = "slick.jdbc.DatabaseUrlDataSource"
       |      numThreads = 1
       |      db {
       |        driver = "org.h2.Driver"
       |        url = "jdbc:h2:mem:$dbName"
       |        user = ""
       |        password = ""
       |      }
       |    }
       |  }
       |  pipeline {
       |    parsers.sql {
       |      enabled = true
       |      input-kinds = ["sql"]
       |    }
       |    outputs.console.enabled = false
       |    outputs.database {
       |      enabled = true
       |      batch-size = 50
       |      timeout = 10
       |    }
       |  }
       |}
    """.stripMargin)

  "A SqlParser" should "write parsed data to a database" in {
    implicit val out: PrintStream = System.out
    implicit val rootConfig: TypesafeConfig = inMemoryDbConfig(Random.alphanumeric.take(10).toString)
    implicit val fs: FileSystem = new Fixture(Seq("fake/script/fileA.sql")).fs
    val superglue = SuperglueRepository(rootConfig).get

    // Initialize database for test
    Await.result(superglue.initialize(testMode = true), 1 second)

    val testProvider = TestScriptProvider(List(
      TestScriptInput("fake/script/fileA.sql", "sql", None,
        """
          |INSERT INTO output_table SELECT * FROM input_table
        """.stripMargin),
    ))
    val source = new Source(testProvider)
    val pipeline = new ParsingPipeline(source)
    val sink = new Sink(pipeline)
    val consumers = sink.drain()
    assert(consumers("database").isInstanceOf[DatabaseConsumer])

    // Query the entities that the parser inserted
    val query = for {
      scripts <- superglue.scriptRepository.getAll
      statements <- superglue.statementRepository.getAll
      tables <- superglue.tableRepository.getAll
      scriptTableRelations <- superglue.scriptTableRepository.getAll
      statementTableRelations <- superglue.statementTableRepository.getAll
    } yield (scripts, statements, tables, scriptTableRelations, statementTableRelations)
    val (scripts, statements, tables, scriptTableRelations, statementTableRelations) = Await.result(query, 1 second)

    assert(scripts.size == 1)
    assert(statements.size == 1)
    assert(tables.size == 2)
    assert(scriptTableRelations.size == 2)
    assert(statementTableRelations.size == 2)

    assert(scripts.exists { script =>
      script.name == "fake/script/fileA.sql" &&
      script.scriptType == "SQL"
    })

    assert(tables.exists(_.name == "OUTPUT_TABLE"))
    assert(tables.exists(_.name == "INPUT_TABLE"))

    assert(statements.exists { statement =>
      statement.text.trim == "INSERT INTO output_table SELECT * FROM input_table" &&
      statement.statementType == "INSERT"
    })
  }
}
