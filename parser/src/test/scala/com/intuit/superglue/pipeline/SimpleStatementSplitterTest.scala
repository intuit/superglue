package com.intuit.superglue.pipeline

import com.intuit.superglue.pipeline.parsers.SimpleStatementSplitter

import scala.collection.JavaConverters._
import org.scalatest.FlatSpec

class SimpleStatementSplitterTest extends FlatSpec {

  "A simple statement splitter" should "find one statement in a script with no semicolons" in {
    val script = "SELECT * from customers"
    val statements = SimpleStatementSplitter.splitStatements(script).asScala.toList
    val expected = List("SELECT * from customers")
    assert(statements == expected)
  }

  it should "remove a semicolon at the end of a single-statement script" in {
    val script = "SELECT * from customers  ;  "
    val statements = SimpleStatementSplitter.splitStatements(script).asScala.toList
    val expected = List("SELECT * from customers")
    assert(statements == expected)
  }

  it should "split two statements and remove a trailing semicolon" in {
    val script =
      """CREATE TABLE products;
        |INSERT INTO products
        |SELECT * from prototype_products;
      """.stripMargin
    val statements = SimpleStatementSplitter.splitStatements(script).asScala.toList
    val expected = List(
      "CREATE TABLE products",
      """INSERT INTO products
        |SELECT * from prototype_products
      """.stripMargin.trim
    )
    assert(statements == expected)
  }

  it should "ignore semicolons that appear in single-quotes" in {
    val script = "SELECT name FROM customers WHERE name = 'semicolon;man';"
    val statements = SimpleStatementSplitter.splitStatements(script).asScala.toList
    val expected = List("SELECT name FROM customers WHERE name = 'semicolon;man'")
    assert(statements == expected)
  }
}
