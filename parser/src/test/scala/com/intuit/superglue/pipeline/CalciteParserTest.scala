package com.intuit.superglue.pipeline

import com.intuit.superglue.pipeline.parsers.CalciteStatementParser
import org.scalatest.FlatSpec

class CalciteParserTest extends FlatSpec {

  val parser = new CalciteStatementParser()

  "A CalciteStatementParser" should "produce a StatementMetadataFragment for a statement" in {
    val metadata = parser.parseStatement("SELECT * FROM some_table")
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("SOME_TABLE"))
  }

  it should "parse a CREATE TABLE statement" in {
    val metadata = parser.parseStatement("CREATE TABLE tableA (column1 int)")
    assert(metadata.statementType.contains("CREATE_TABLE"))
    assert(metadata.outputObjects.contains("TABLEA"))
  }

  it should "parse a CREATE TABLE statement with a SELECT sub-query" in {
    val create_then_select = parser.parseStatement("CREATE TABLE table_A AS SELECT * FROM template_table")
    assert(create_then_select.statementType.contains("CREATE_TABLE"))
    assert(create_then_select.outputObjects.contains("TABLE_A"))
    assert(create_then_select.inputObjects.contains("TEMPLATE_TABLE"))
  }

  it should "parse a CREATE TABLE statement with a WITH sub-query" in {
    val create_then_with = parser.parseStatement(
      """
        |CREATE TABLE table_B AS
        |  WITH temp_query AS (SELECT * FROM input_table)
        |  SELECT * FROM other_table
      """.stripMargin)
    assert(create_then_with.statementType.contains("CREATE_TABLE"))
    assert(create_then_with.inputObjects.contains("INPUT_TABLE"))
    assert(create_then_with.outputObjects.contains("TABLE_B"))
  }

  it should "parse a CREATE TABLE statement with a UNION sub-query" in {
    val create_then_union = parser.parseStatement(
      """
        |CREATE TABLE table_C AS
        |  SELECT * FROM input_Z
        |    UNION
        |  SELECT * FROM input_Y
      """.stripMargin)
    assert(create_then_union.statementType.contains("CREATE_TABLE"))
    assert(create_then_union.inputObjects.contains("INPUT_Z"))
    assert(create_then_union.inputObjects.contains("INPUT_Y"))
    assert(create_then_union.outputObjects.contains("TABLE_C"))
  }

  it should "parse a CREATE TABLE statement with an ORDER_BY sub-query" in {
    val create_then_orderby = parser.parseStatement(
      """
        |CREATE TABLE table_D AS
        |  SELECT * FROM input_X
        |  ORDER BY columnG ASC
      """.stripMargin)
    assert(create_then_orderby.statementType.contains("CREATE_TABLE"))
    assert(create_then_orderby.inputObjects.contains("INPUT_X"))
    assert(create_then_orderby.outputObjects.contains("TABLE_D"))
  }

  it should "parse a CREATE VIEW statement" in {
    val metadata = parser.parseStatement("CREATE VIEW tmp AS SELECT columnA, columnB FROM input_table")
    assert(metadata.statementType.contains("CREATE_VIEW"))
    assert(metadata.inputObjects.contains("INPUT_TABLE"))
    assert(metadata.outputObjects.contains("TMP"))
  }

  it should "parse a CREATE VIEW statement with a SELECT sub-query" in {
    val create_then_with = parser.parseStatement(
      """
        |CREATE VIEW view_A AS
        |  WITH temp_view AS (SELECT * FROM model_table)
        |  SELECT * FROM temp_view
      """.stripMargin)
    assert(create_then_with.statementType.contains("CREATE_VIEW"))
    assert(create_then_with.inputObjects.contains("MODEL_TABLE"))
    assert(create_then_with.inputObjects.contains("TEMP_VIEW"))
    assert(create_then_with.outputObjects.contains("VIEW_A"))
  }

  it should "parse a CREATE VIEW statement with an ORDER_BY sub-query" in {
    val create_then_orderby = parser.parseStatement(
      """
        |CREATE VIEW view_B AS
        |  SELECT * FROM input_W
        |  ORDER BY columnH DESC
      """.stripMargin)
    assert(create_then_orderby.statementType.contains("CREATE_VIEW"))
    assert(create_then_orderby.inputObjects.contains("INPUT_W"))
    assert(create_then_orderby.outputObjects.contains("VIEW_B"))
  }

  it should "parse an INSERT statement with a SELECT sub-query" in {
    val metadata = parser.parseStatement("INSERT INTO output_table SELECT * FROM input_table")
    assert(metadata.statementType.contains("INSERT"))
    assert(metadata.inputObjects.contains("INPUT_TABLE"))
    assert(metadata.outputObjects.contains("OUTPUT_TABLE"))
  }

  it should "parse an INSERT statement with a WITH sub-query" in {
    val metadata = parser.parseStatement(
      """
        |INSERT INTO output_A
        |  WITH temp AS (SELECT * FROM model_tableB)
        |  SELECT * FROM temp
      """.stripMargin)
    assert(metadata.statementType.contains("INSERT"))
    assert(metadata.inputObjects.contains("MODEL_TABLEB"))
    assert(metadata.inputObjects.contains("TEMP"))
    assert(metadata.outputObjects.contains("OUTPUT_A"))
  }

  it should "parse an INSERT statement with a VALUES clause" in {
    val metadata = parser.parseStatement(
      """
        |INSERT INTO output_B
        |  VALUES (a, b, c, d)
      """.stripMargin)
    assert(metadata.statementType.contains("INSERT"))
    assert(metadata.outputObjects.contains("OUTPUT_B"))
    assert(metadata.inputObjects.isEmpty)
  }

  it should "parse an INSERT statement with a UNION sub-query" in {
    val metadata = parser.parseStatement(
      """
        |INSERT INTO output_C
        |  SELECT * FROM input_X
        |    UNION
        |  SELECT * FROM input_Y
      """.stripMargin)
    assert(metadata.statementType.contains("INSERT"))
    assert(metadata.inputObjects.contains("INPUT_X"))
    assert(metadata.inputObjects.contains("INPUT_Y"))
    assert(metadata.outputObjects.contains("OUTPUT_C"))
  }

  it should "parse an INSERT statement with an ORDER BY sub-query" in {
    val metadata = parser.parseStatement(
      """
        |INSERT INTO output_D
        |  SELECT * FROM input_W
        |  ORDER BY columnB ASC
      """.stripMargin)
    assert(metadata.statementType.contains("INSERT"))
    assert(metadata.inputObjects.contains("INPUT_W"))
    assert(metadata.outputObjects.contains("OUTPUT_D"))
  }

  it should "parse a SELECT statement with an AS alias" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM input_A AS input_B
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT_A"))
  }

  it should "parse a SELECT statement with a JOIN sub-query" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        |  input_A INNER JOIN input_B
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT_A"))
    assert(metadata.inputObjects.contains("INPUT_B"))
  }

  it should "parse a SELECT statement with a JOIN with an AS" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        |  input_A as A INNER JOIN input_B as B
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT_A"))
    assert(metadata.inputObjects.contains("INPUT_B"))
  }

  it should "parse a SELECT statement with a JOIN with a child SELECT" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        |  (SELECT * FROM input_A)
        |    INNER JOIN
        |  input_B
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT_A"))
    assert(metadata.inputObjects.contains("INPUT_B"))
  }

  it should "parse a SELECT statement with a JOIN with a child JOIN" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        |  input_A INNER JOIN
        |  input_B INNER JOIN
        |  input_C
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT_A"))
    assert(metadata.inputObjects.contains("INPUT_B"))
    assert(metadata.inputObjects.contains("INPUT_C"))
  }

  it should "parse an AS alias on a SELECT statement" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        |  (SELECT * FROM input) as selection
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT"))
  }

  it should "parse an AS alias on a UNION statement" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        |  (SELECT * FROM input_A
        |    UNION
        |  SELECT * FROM input_B) as theUnion
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT_A"))
    assert(metadata.inputObjects.contains("INPUT_B"))
  }

  it should "parse an AS alias on a WITH statement" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        | (WITH temp AS (SELECT * FROM input)
        |  SELECT * FROM temp) as theWith
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT"))
  }

  it should "parse an AS alias on an ORDER BY statement" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM
        |  (SELECT * FROM input_A ORDER BY column_B ASC) as ordered
      """.stripMargin)
    assert(metadata.statementType.contains("SELECT"))
    assert(metadata.inputObjects.contains("INPUT_A"))
  }

  it should "parse a UNION statement with a child UNION" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM input_A
        |  UNION
        |SELECT * FROM input_B
        |  UNION
        |SELECT * FROM input_C
      """.stripMargin)
    assert(metadata.statementType.contains("UNION"))
    assert(metadata.inputObjects.contains("INPUT_A"))
    assert(metadata.inputObjects.contains("INPUT_B"))
    assert(metadata.inputObjects.contains("INPUT_C"))
  }

  it should "parse an UPDATE statement" in {
    val metadata = parser.parseStatement(
      """
        |UPDATE output_table SET
        |  column1 = value1,
        |  column2 = value2
      """.stripMargin)
    assert(metadata.statementType.contains("UPDATE"))
    assert(metadata.outputObjects.contains("OUTPUT_TABLE"))
  }

  it should "parse a MERGE statement" in {
    val metadata = parser.parseStatement(
      """
        |MERGE INTO target_table tt USING source_table st
        |  ON tt.key = st.key
        |  WHEN MATCHED THEN
        |    UPDATE SET column1 = value1
        |  WHEN NOT MATCHED THEN
        |    INSERT VALUES ('hello', 'world')
      """.stripMargin)
    assert(metadata.statementType.contains("MERGE"))
    assert(metadata.inputObjects.contains("SOURCE_TABLE"))
    assert(metadata.outputObjects.contains("TARGET_TABLE"))
  }

  it should "parse an ORDER BY statement" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM input_table
        |  ORDER BY column1, column2 DESC
      """.stripMargin)
    assert(metadata.statementType.contains("ORDER_BY"))
    assert(metadata.inputObjects.contains("INPUT_TABLE"))
  }

  it should "parse an ORDER BY statement on a UNION" in {
    val metadata = parser.parseStatement(
      """
        |SELECT * FROM input_one
        |  UNION
        |SELECT * FROM input_two
        |ORDER BY column1, column2 ASC
      """.stripMargin)
    assert(metadata.statementType.contains("ORDER_BY"))
    assert(metadata.inputObjects.contains("INPUT_ONE"))
    assert(metadata.inputObjects.contains("INPUT_TWO"))
  }

  it should "parse a WITH statement" in {
    val metadata = parser.parseStatement(
      """
        |WITH
        |  A as (SELECT * FROM table_A),
        |  BC as (SELECT * FROM table_B UNION SELECT * FROM table_C),
        |  D as (SELECT * FROM table_D ORDER BY columnA, columnB DESC)
        |SELECT * FROM D
      """.stripMargin)
    assert(metadata.statementType.contains("WITH"))
    assert(metadata.inputObjects.contains("TABLE_A"))
    assert(metadata.inputObjects.contains("TABLE_B"))
    assert(metadata.inputObjects.contains("TABLE_C"))
    assert(metadata.inputObjects.contains("TABLE_D"))
  }
}
