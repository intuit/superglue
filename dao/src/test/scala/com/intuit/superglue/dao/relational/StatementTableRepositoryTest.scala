package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model.PrimaryKeys._
import com.intuit.superglue.dao.model.{Direction, StatementEntity, StatementTableJoin, TableEntity}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class StatementTableRepositoryTest extends InMemoryDataSpec {

  "A StatementTableRepository" should "insert a statement and all of its related tables" in Fixture { f =>
    val table = TableEntity("the_best_table", "table_schema", "the_platform")
    val statement = StatementEntity("SELECT", "SELECT * FROM the_best_table", ScriptPK(0), tables = Set((Direction.input, Set(table))))

    val (statementCount, tableCount, relationCount) = Await.result(f.statementTableRepo.addStatementWithTables(statement), 1 second)
    assert(statementCount == 1)
    assert(tableCount == 1)
    assert(relationCount == 1)

    val action = for {
      ss <- f.statementRepo.getAll
      ts <- f.tableRepo.getAll
      rs <- f.statementTableRepo.getAll
    } yield (ss, ts, rs)
    val (insertedStatements, insertedTables, insertedRelations) = Await.result(action, 1 second)
    assert(insertedTables.contains(table))
    assert(insertedStatements.exists(s => s.id == statement.id))
    assert(insertedRelations.exists(r => r.tableId == table.id))
    assert(insertedRelations.exists(r => r.statementId == statement.id))
  }

  it should "join statements and tables" in Fixture { f =>
    val tables = Seq(
      TableEntity("table_one", "table_schema", "VERTICA"),
      TableEntity("table_two", "table_schema", "VERTICA"),
      TableEntity("table_three", "table_schema", "VERTICA"),
      TableEntity("table_four", "table_schema", "VERTICA"),
      TableEntity("table_five", "table_schema", "VERTICA"),
    )

    val tablesOne = Set((Direction.input, tables.take(3).toSet))
    val tablesTwo = Set((Direction.output, tables.drop(3).toSet))

    val statements = Set(
      StatementEntity("SELECT", "SELECT * from table_one", ScriptPK(0), tables = tablesOne),
      StatementEntity("SELECT", "SELECT * from table_two", ScriptPK(1), tables = tablesOne),
      StatementEntity("SELECT", "SELECT * from table_three", ScriptPK(2), tables = tablesOne),
      StatementEntity("INSERT", "INSERT INTO table_four VALUES(a, b)", ScriptPK(3), tables = tablesTwo),
      StatementEntity("INSERT", "INSERT INTO table_five VALUES(b, c)", ScriptPK(4), tables = tablesTwo),
    )

    val (statementCount, tableCount, relationCount) = Await.result(f.statementTableRepo.addStatementsWithTables(statements), 1 second)
    assert(statementCount == 5)
    assert(tableCount == 5)
    assert(relationCount == 13)

    val expectedEntities = Seq(
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_one", ScriptPK(0), 0, "TABLE_ONE", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_one", ScriptPK(0), 0, "TABLE_TWO", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_one", ScriptPK(0), 0, "TABLE_THREE", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_two", ScriptPK(1), 0, "TABLE_ONE", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_two", ScriptPK(1), 0, "TABLE_TWO", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_two", ScriptPK(1), 0, "TABLE_THREE", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_three", ScriptPK(2), 0, "TABLE_ONE", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_three", ScriptPK(2), 0, "TABLE_TWO", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.input, "SELECT", "SELECT * from table_three", ScriptPK(2), 0, "TABLE_THREE", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.output, "INSERT", "INSERT INTO table_four VALUES(a, b)", ScriptPK(3), 0, "TABLE_FOUR", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.output, "INSERT", "INSERT INTO table_four VALUES(a, b)", ScriptPK(3), 0, "TABLE_FIVE", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.output, "INSERT", "INSERT INTO table_five VALUES(b, c)", ScriptPK(4), 0, "TABLE_FOUR", "TABLE_SCHEMA", "VERTICA"),
      StatementTableJoin(StatementPK(0), TablePK(0), Direction.output, "INSERT", "INSERT INTO table_five VALUES(b, c)", ScriptPK(4), 0, "TABLE_FIVE", "TABLE_SCHEMA", "VERTICA"),
    )

    def eqButId(one: StatementTableJoin, two: StatementTableJoin): Boolean = {
      one.direction == two.direction &&
      one.statementType == two.statementType &&
      one.statementText == two.statementText &&
      one.statementVersionId == two.statementVersionId &&
      one.statementScript == two.statementScript &&
      one.tableName == two.tableName &&
      one.tableSchema == two.tableSchema &&
      one.tablePlatform == two.tablePlatform
    }

    val actualEntities = Await.result(f.statementTableRepo.getAllJoined, 1 second)
    assert(actualEntities.size == 13)
    expectedEntities.foreach { expected =>
      assert(actualEntities.exists(actual => eqButId(expected, actual)))
    }
  }
}
