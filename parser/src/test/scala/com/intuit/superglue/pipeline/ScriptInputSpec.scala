package com.intuit.superglue.pipeline

import java.io.{ByteArrayInputStream, InputStream}

import com.intuit.superglue.pipeline.producers.{ScriptInput, ScriptProvider}
import org.scalatest.FlatSpec

import scala.util.Try

trait ScriptInputSpec extends FlatSpec {

  /** A TestInput holds an inputName and inputKind, but never gives an InputStream */
  protected case class TestScriptInput(
    name: String,
    kind: String,
    dialect: Option[String],
    testScript: String,
  ) extends ScriptInput {
    override def source: String = "TEST"
    override def readInputStream[R](f: InputStream => R): Try[R] = Try(f(new ByteArrayInputStream(testScript.getBytes())))
  }

  /** A TestInputProvider provides an in-memory list of Inputs as a stream */
  protected case class TestScriptProvider(inputs: List[ScriptInput]) extends ScriptProvider {
    override def stream(): Iterator[ScriptInput] = inputs.iterator
  }
}
