package com.intuit.superglue.cli

import java.io.PrintStream
import java.nio.file.{FileSystem, Files, Path}
import java.sql.SQLSyntaxErrorException

import com.intuit.superglue.dao.SuperglueRepository
import com.typesafe.config.ConfigFactory
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec

import scala.annotation.meta.field
import scala.concurrent.{Await, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

@CommandLine.Command(name = "init",
  description = Array("Set up and interact with superglue's services"))
class InitCli(implicit out: PrintStream, fs: FileSystem) extends Runnable {

  @(CommandLine.Spec@field)
  var spec: CommandSpec = _

  @(CommandLine.Option@field)(
    names = Array("-c", "--config"),
    description = Array("A custom config file to use for execution"))
  var config: Path = fs.getPath("./superglue.conf")

  @(CommandLine.Option@field)(
    names = Array("--db", "--database"))
  var initDatabase: Boolean = false

  @(CommandLine.Option@field)(
    names = Array("-t", "--timeout"),
    description = Array("Seconds to wait for an operation (default 10)"))
  var timeout: Int = 10

  override def run(): Unit = {

    // Workaround: Picocli always uses the default Filesystem, rather than the custom
    // one provided by the "fs" parameter (needed for testing)
    // We just stringify the path and then resolve it on the fs we want
    val configFile: Path = fs.getPath(config.toString)

    // If the given (or default) configuration is not found, print usage and quit
    val conf = if (!Files.exists(configFile)) {
      out.println(s"Configuration file '${configFile.toString}' not found. Using default configuration")
      ConfigFactory.load()
    } else {
      val userConf = ConfigFactory.parseReader(Files.newBufferedReader(configFile))
      if (!userConf.hasPath("com.intuit.superglue.dao")) {
        out.println("Warning: Given configuration does not have 'com.intuit.superglue.dao'")
      }
      userConf.withFallback(ConfigFactory.load())
    }

    if (initDatabase) {
      val repo = SuperglueRepository(conf).get
      try {
        val action = repo.initialize().transform {
          case Failure(_: SQLSyntaxErrorException) => Success(false)
          case Failure(e) => Failure(e)
          case Success(_) => Success(true)
        }
        val success = Await.result(action, timeout second)
        if (success) out.println("Superglue initialized!")
        else out.println("Superglue is already initialized")
      } catch {
        case _: TimeoutException => out.println("Timed out while attempting to connect to Superglue")
      }
      return
    }

    new CommandLine(spec).usage(out)
  }
}
