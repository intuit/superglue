package com.intuit.superglue.pipeline

import com.intuit.superglue.pipeline.parsers.SqlScriptParser
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class ParsingPipelineTest extends ScriptInputSpec with FsSpec {

  "The Pipeline" should "send inputs to parsers according to inputKind configurations" in {
    val files = Seq(
      "fake/script/fileA.sql",
      "fake/script/fileB.hql",
      "nonsense/fileC.xql",
      "nonsense/fileD.blah",
    )

    Fixture(files: _*) { implicit fs =>
      implicit val rootConfig: TypesafeConfig = ConfigFactory.parseString(
        """
          |com.intuit.superglue.pipeline.parsers {
          |  sql {
          |    enabled = true
          |    input-kinds = ["sql"]
          |  }
          |  sqlHive {
          |    enabled = true
          |    input-kinds = ["sql_hive"]
          |  }
          |  sqlXray {
          |    input-kinds = ["sql_xray"]
          |  }
          |}
        """.stripMargin)

      // Create a test InputProvider with hardcoded "inputs"
      val testProvider = TestScriptProvider(List(
        TestScriptInput("fake/script/fileA.sql", "sql", "create table output like input including projections"),
        TestScriptInput("fake/script/fileB.hql", "sql_hive", "some hive sql script"),
        TestScriptInput("nonsense/fileC.xql", "sql_xray", "some nonexistant sql strain script"),
        TestScriptInput("nonsense/fileD.blah", "nonsense", "not even a sql file"),
      ))

      // Create a Source that reads from the testProvider
      val source = new Source(testProvider)

      // Use the test Source to feed the Processor
      val processor = new ParsingPipeline(source)

      // Outputs record which Parser got them. Verify they went to the right ones.
      val futureOutputs = processor.stream().toSeq
      val expectedOutputs = Seq(
        ("fake/script/fileA.sql", new SqlScriptParser().parserName),
      )
      val outputs = Await.result(Future.sequence(futureOutputs), 1 second)
      assert(outputs.length == expectedOutputs.length)
      outputs.zip(expectedOutputs).foreach { case (output, (inputName, parserName)) =>
        assert(output.scriptName == inputName)
        assert(output.scriptParser == parserName)
      }
    }
  }
}
