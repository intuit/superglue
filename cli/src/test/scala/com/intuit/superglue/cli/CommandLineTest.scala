package com.intuit.superglue.cli

import java.nio.file.Files

import com.intuit.superglue.pipeline.consumers.ReportingConsumer.Report
import org.scalatest.Matchers
import play.api.libs.json._

class CommandLineTest extends CliSpec with Matchers {

  "The command line client" should "print a help message" in {
    Fixture { f =>
      val args = Array("--help")
      f.main.run(args)
      assert(f.stdoutString.contains("Usage: superglue"))
      assert(f.stdoutString.contains("parse"))
    }

    Fixture { f =>
      val args = Array.empty[String]
      f.main.run(args)
      assert(f.stdoutString.contains("Usage: superglue"))
      assert(f.stdoutString.contains("parse"))
    }
  }

  it should "parse scripts according to a configuration" in Fixture { f =>
    val confFile = f.root.resolve("/application.conf")
    Files.createFile(confFile)
    val conf =
      """
        |com.intuit.superglue {
        |  pipeline.inputs.files = [{
        |    base = "/repository/"
        |    includes = [ "glob:**.sql" ]
        |  }]
        |}
      """.stripMargin
    Files.write(confFile, conf.getBytes)

    // Create a virtual "repository" to put our virtual SQL scripts in
    val repositoryDirectory = f.root.resolve("repository/")
    Files.createDirectories(repositoryDirectory)
    val scripts = Seq(
      ("testA.sql", """SELECT * FROM input_table_A"""),
      ("testB.sql", """INSERT INTO output_table_B SELECT * FROM input_table_C"""))

    // Create each virtual SQL file and write our virtual SQL to them
    scripts.foreach { case (filename, contents) =>
      val scriptFile = repositoryDirectory.resolve(filename)
      Files.createFile(scriptFile)
      Files.write(scriptFile, contents.getBytes)
    }

    // Run the command line with
    val args = Array("parse", "-c", "/application.conf")
    f.main.run(args)

    // Check that the output is valid JSON
    val metadata = Json.parse(f.stdoutString)
    assert(metadata.isInstanceOf[JsArray])
    val metadataArray = metadata.asInstanceOf[JsArray].value
    assert(metadataArray.size == 2)

    val expectedOne = JsObject(Map(
      "name" -> JsString("testA.sql"),
      "statements" -> JsArray(Seq(
        JsObject(Map(
          "type" -> JsString("SELECT"),
          "inputObjects" -> JsArray(Seq(JsString("INPUT_TABLE_A"))),
          "outputObjects" -> JsArray(),
        ))
      )),
    ))

    val expectedTwo = JsObject(Map(
      "name" -> JsString("testB.sql"),
      "statements" -> JsArray(Seq(
        JsObject(Map(
          "type" -> JsString("INSERT"),
          "inputObjects" -> JsArray(Seq(JsString("INPUT_TABLE_C"))),
          "outputObjects" -> JsArray(Seq(JsString("OUTPUT_TABLE_B"))),
        ))
      ))
    ))

    assert(metadataArray.contains(expectedOne))
    assert(metadataArray.contains(expectedTwo))
  }

  it should "print a message when a configuration file is not found for parsing" in Fixture { f =>
    val args = Array("parse", "-c", "./application.conf")
    f.main.run(args)
    assert(f.stdoutString.startsWith("Configuration file './application.conf' not found"))
  }

  it should "print a report about the scripts parsed" in Fixture { f =>
    val confFile = f.root.resolve("/application.conf")
    Files.createFile(confFile)
    val conf =
      """
        |com.intuit.superglue {
        |  pipeline.inputs.files = [{
        |    base = "/repository/"
        |    includes = [ "glob:**.sql" ]
        |  }]
        |}
      """.stripMargin
    Files.write(confFile, conf.getBytes)

    // Create a virtual "repository" to put our virtual SQL scripts in
    val repositoryDirectory = f.root.resolve("repository/")
    Files.createDirectories(repositoryDirectory)
    val scripts = Seq(
      ("testA.sql", """SELECT * FROM input_table_A"""),
      ("testB.sql", """INSERT INTO output_table_B SELECT * FROM input_table_C"""))

    // Create each virtual SQL file and write our virtual SQL to them
    scripts.foreach { case (filename, contents) =>
      val scriptFile = repositoryDirectory.resolve(filename)
      Files.createFile(scriptFile)
      Files.write(scriptFile, contents.getBytes)
    }

    // Run the command line with
    val args = Array("parse", "--report", "-c", "/application.conf")
    f.main.run(args)

    val report = Json.parse(f.stdoutString)
    assert(report.validate[Report].isSuccess)
  }

  it should "print the names of the inputs without parsing them when passed --printInputs" in Fixture { f =>
    val confFile = f.root.resolve("/application.conf")
    Files.createFile(confFile)
    val conf =
      """
        |com.intuit.superglue {
        |  pipeline.inputs.files = [{
        |    base = "/repository/"
        |    includes = [ "glob:**.sql" ]
        |  }]
        |}
      """.stripMargin
    Files.write(confFile, conf.getBytes)

    // Create a virtual "repository" to put our virtual SQL scripts in
    val repositoryDirectory = f.root.resolve("repository/")
    Files.createDirectories(repositoryDirectory)
    val scripts = Seq(
      ("testA.sql", """SELECT * FROM input_table_A"""),
      ("testB.sql", """INSERT INTO output_table_B SELECT * FROM input_table_C"""))

    // Create each virtual SQL file and write our virtual SQL to them
    scripts.foreach { case (filename, contents) =>
      val scriptFile = repositoryDirectory.resolve(filename)
      Files.createFile(scriptFile)
      Files.write(scriptFile, contents.getBytes)
    }

    // Run the command line with
    val args = Array("parse", "--printInputs", "-c", "/application.conf")
    f.main.run(args)

    val lines = f.stdoutString.lines.toSeq
    assert(lines.size == 2)
    assert(lines.contains("testA.sql"))
    assert(lines.contains("testB.sql"))
  }

  it should "print the elastic subcommand usage when given no arguments" in Fixture { f =>
    val args = Array("elastic")
    f.main.run(args)
    assert(f.stdoutString.startsWith("Usage: superglue elastic"))
  }
}
