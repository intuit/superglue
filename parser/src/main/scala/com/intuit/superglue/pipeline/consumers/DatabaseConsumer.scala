package com.intuit.superglue.pipeline.consumers

import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.SuperglueRepository
import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.consumers.OutputConsumer.{EndOfStream, Message, StartOfStream}
import com.typesafe.config.{Config => TypesafeConfig}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * A consumer which delivers the parsed metadata to a database.
  *
  * The driving use-case of this component is to connect to a MySQL database, but the
  * DAO library is rather modular, and has other databases it can interact with.
  * None of the other options have been tested, but nothing here should by MySQL-specific,
  * so swapping out the driver should "just work".
  *
  * @param rootConfig The configuration used to connect to the database.
  */
class DatabaseConsumer()(implicit rootConfig: TypesafeConfig) extends OutputConsumer[Future[ScriptMetadata]] {
  private val databaseConfig = rootConfig.getConfig("com.intuit.superglue.pipeline.outputs.database")
  private val enabled = databaseConfig.getBoolean("enabled")
  private val batchSize = databaseConfig.getInt("batch-size")
  private val timeout = databaseConfig.getInt("timeout")
  private val sg = SuperglueRepository(rootConfig).get

  /**
    * A buffer is a set of metadata to be committed to the database in one session.
    */
  private var buffer = Seq.empty[Future[(ScriptEntity, Set[StatementEntity])]]

  /**
    * Committing each buffer to the database also takes time. Batches is a future
    * which is only complete when all of the buffers have been sent.
    */
  private var batches = Seq.empty[Future[Unit]]

  override def accept(event: OutputConsumer.Event[Future[ScriptMetadata]]): Unit = {
    if (!enabled) return
    event match {
      case StartOfStream(_) =>
      case Message(futureMetadata) =>

        def getStmtTables(fragment: StatementMetadataFragment, selector: StatementMetadataFragment => List[String]) =
          selector(fragment).flatMap(_.split(","))

        def getScriptTables(metadata: ScriptMetadata, selector: StatementMetadataFragment => List[String]) =
          metadata.statementsMetadata.flatMap(stmt => selector(stmt.statementMetadataFragment))

        val event = futureMetadata.map { metadata =>

          // Create lists of TableEntities representing input and output tables
          val inputTableEntities = getScriptTables(metadata, _.inputObjects).map(TableEntity(_, "", ""))
          val outputTableEntities = getScriptTables(metadata, _.outputObjects).map(TableEntity(_, "", ""))

          // Create a ScriptEntity representing this script
          val scriptEntity = ScriptEntity(
            metadata.scriptName,
            metadata.scriptKind,
            scriptGitUrl = "",
            scriptHash = "###",
            tables = Set(
              (Direction.input, inputTableEntities.toSet),
              (Direction.output, outputTableEntities.toSet),
            ),
          )

          val statementEntities = metadata.statementsMetadata.map { statement =>
            val inputTables = getStmtTables(statement.statementMetadataFragment, _.inputObjects)
            val outputTables = getStmtTables(statement.statementMetadataFragment, _.outputObjects)
            val statementType = statement.statementMetadataFragment.statementType
            if (statementType == UNKNOWN_STATEMENT_TYPE) {
              None
            } else {
              Some(StatementEntity(
                statementType = statementType,
                text = statement.statementText,
                scriptId = scriptEntity.id,
                tables = Set(
                  (Direction.input, inputTables.map(TableEntity(_, "", "")).toSet),
                  (Direction.output, outputTables.map(TableEntity(_, "", "")).toSet),
                )
              ))
            }
          }.collect { case Some(statementEntity) => statementEntity }

          (scriptEntity, statementEntities.toSet)
        }

        buffer :+= event
        if (buffer.size >= batchSize) {
          flush(buffer)
          buffer = Seq.empty
        }
      case EndOfStream =>
        flush(buffer)
        Await.result(Future.sequence(batches), timeout seconds)
    }
  }

  private def flush(buffer: Seq[Future[(ScriptEntity, Set[StatementEntity])]]): Unit = {
    batches :+= Future.sequence(buffer).flatMap { buffer =>
      val scripts = buffer.map { case (script, _) => script }
      val statements = buffer.flatMap { case (_, stmts) => stmts }
      for {
        _ <- sg.scriptTableRepository.addScriptsWithTables(scripts.toSet)
        _ <- sg.statementTableRepository.addStatementsWithTables(statements.toSet)
      } yield ()
    }
  }
}
