package com.intuit.superglue.cli

import java.io.PrintStream
import java.nio.file.{FileSystem, Path, Paths}

import com.intuit.superglue.dao.SuperglueRepository
import com.intuit.superglue.elastic.ElasticService
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec

import scala.annotation.meta.field
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

@CommandLine.Command(name = "elastic",
  description = Array("Uploads superglue database entities to elasticsearch"))
class ElasticCli(implicit out: PrintStream, fs: FileSystem) extends Runnable {

  @(CommandLine.Spec@field)
  var spec: CommandSpec = _

  @(CommandLine.Option@field)(
    names = Array("-c", "--config"),
    description = Array("A custom config file to use for execution")
  )
  var config: Path = Paths.get("./superglue.conf")

  @(CommandLine.Option@field)(
    names = Array("--load"),
    description = Array("Load entities from Superglue into elasticsearch")
  )
  var load: Boolean = false

  @(CommandLine.Option@field)(
    names = Array("-t", "--timeout"),
    description = Array("Seconds to wait for completion (default is 10)")
  )
  var timeout: Int = 10

  @(CommandLine.Option@field)(
    names = Array("-h", "--help"),
    description = Array("Print this help message"),
    usageHelp = true)
  var help: Boolean = false

  override def run(): Unit = {
    val conf: TypesafeConfig = ConfigFactory
      .parseFile(config.toFile)
      .withFallback(ConfigFactory.load())

    val repository = SuperglueRepository(conf).get
    val elasticService = new ElasticService(conf)

    if (load) {
      elasticService.perform { implicit client =>
        val action = for {
          _ <- elasticService.createIndexIfNotExists.map {
            case None => out.println("Index already exists, skipping index initialization")
            case Some(_) => out.println("Initialized index")
          }
          tables <- repository.tableRepository.getAll
          _ <- elasticService.createIndexAlias
          _ <- Future {
            out.println(s"Found ${tables.size} tables to insert to index")
          }
          _ <- Future.sequence(
            elasticService.uploadTables(tables)
              .map(_.map { response =>
                out.println(s"Batch index insert: ${response.result.successes.size} succeeded and ${response.result.failures.size} failed")
              })
          )
        } yield ()
        Await.result(action, timeout second)
      }
      return
    }

    new CommandLine(spec).usage(out)
  }
}
