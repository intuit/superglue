package com.intuit.superglue.cli

import java.io.PrintStream
import java.nio.file.{FileSystem, FileSystems}

import com.intuit.superglue.cli.Main.Cli
import picocli.CommandLine
import picocli.CommandLine.{PicocliException, RunAll}

import scala.annotation.meta.field

object Main {
  def main(args: Array[String]): Unit = {
    new Main().run(args)
  }

  @CommandLine.Command(name = "superglue")
  case class Cli(

    @(CommandLine.Option@field)(
      names = Array("-h", "--help"),
      description = Array("Print this help message"),
      usageHelp = true)
    var help: Boolean = false,

  ) extends Runnable {
    // Top-level menu does not have any actions to take
    override def run(): Unit = ()
  }
}

class Main(
  val systemOut: PrintStream = System.out,
  val filesystem: FileSystem = FileSystems.getDefault) {
  implicit val out: PrintStream = systemOut
  implicit val fs: FileSystem = filesystem
  def run(args: Array[String]): Unit = {

    val cmd = new CommandLine(Cli())
      .addSubcommand("parse", new ParserCli())
      .addSubcommand("elastic", new ElasticCli())
      .addSubcommand("init", new InitCli())

    // If no arguments are given, print usage
    if (args.length == 0) {
      cmd.usage(out)
      return
    }

    try {
      cmd.parse(args: _*)
    } catch {
      case _: Exception => return
    }

    if (cmd.isUsageHelpRequested) {
      cmd.usage(out)
      return
    }

    try {
      cmd.parseWithHandler(new RunAll(), args)
    } catch {
      case pe: PicocliException => throw pe.getCause
    }
  }
}
