package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model.PrimaryKeys.{ScriptPK, TablePK}
import com.intuit.superglue.dao.model.{Direction, ScriptEntity, ScriptTableJoin, TableEntity}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ScriptTableRepositoryTest extends InMemoryDataSpec {

  "A ScriptTableRepository" should "insert a script and its tables, and build a relationship" in Fixture { f =>
    val table = TableEntity("table_one", "table_schema", "VERTICA")
    val script = ScriptEntity("script_one", "sql", "https://github.com/some/repo", "###", tables = Set((Direction.input, Set(table))))

    val (scriptCount, tableCount, relationCount) = Await.result(f.scriptTableRepo.addScriptWithTables(script), 1 second)
    assert(scriptCount == 1)
    assert(tableCount == 1)
    assert(relationCount == 1)

    val queryAction = for {
      ss <- f.scriptRepo.getAll
      ts <- f.tableRepo.getAll
      rs <- f.scriptTableRepo.getAll
    } yield (ss, ts, rs)
    val (insertedScripts, insertedTables, insertedRelations) = Await.result(queryAction, 1 second)
    assert(insertedTables.contains(table))
    assert(insertedScripts.exists(s => s.id == script.id))
    assert(insertedRelations.exists(r => r.tableId == table.id))
    assert(insertedRelations.exists(r => r.scriptId == script.id))
    assert(insertedRelations.exists(r => r.direction == Direction.input))
  }

  it should "join tables and scripts" in Fixture { f =>
    val tablesToInsert = Seq(
      TableEntity("table_one", "table_schema", "VERTICA"),
      TableEntity("table_two", "table_schema", "VERTICA"),
      TableEntity("table_three", "table_schema", "VERTICA"),
      TableEntity("table_four", "table_schema", "VERTICA"),
      TableEntity("table_five", "table_schema", "VERTICA"),
    )

    val tablesOne = Set((Direction.input, tablesToInsert.take(3).toSet))
    val tablesTwo = Set((Direction.output, tablesToInsert.drop(3).toSet))

    val scriptsToInsert = Set(
      ScriptEntity("script_one", "sql", "https://github.com/some/repo", "###", tables = tablesOne),
      ScriptEntity("script_two", "sql", "https://github.com/some/repo", "###", tables = tablesOne),
      ScriptEntity("script_three", "sql", "https://github.com/some/repo", "###", tables = tablesOne),
      ScriptEntity("script_four", "sql", "https://github.com/some/repo", "###", tables = tablesTwo),
      ScriptEntity("script_five", "sql", "https://github.com/some/repo", "###", tables = tablesTwo),
    )

    // Creating a relation action can fail if given uninserted entities (i.e. id = None)
    val insertRelationsAction = f.scriptTableRepo.addScriptsWithTables(scriptsToInsert)

    // 3 scripts * 3 input tables = 9, plus 2 scripts * 2 output tables = 4, totaling 13
    val (scriptCount, tableCount, relationCount) = Await.result(insertRelationsAction, 2 second)
    assert(scriptCount == 5)
    assert(tableCount == 5)
    assert(relationCount == 13)

    val expectedJoinedEntities = Seq(
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_one", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_ONE", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_one", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_TWO", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_one", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_THREE", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_two", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_ONE", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_two", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_TWO", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_two", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_THREE", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_three", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_ONE", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_three", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_TWO", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.input,  "script_three", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_THREE", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.output,  "script_four", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_FOUR", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.output,  "script_four", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_FIVE", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.output,  "script_five", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_FOUR", "TABLE_SCHEMA", "VERTICA"),
      ScriptTableJoin(ScriptPK(0L), TablePK(0L), Direction.output,  "script_five", "SQL", "https://github.com/some/repo", "###", 0, "TABLE_FIVE", "TABLE_SCHEMA", "VERTICA"),
    )

    val queryAction = f.scriptTableRepo.getAllJoined
    val queriedJoinedRows = Await.result(queryAction, 2 second)

    // Compare everything except the IDs
    def compareJoin(actual: ScriptTableJoin, expected: ScriptTableJoin): Boolean = {
      actual.direction == expected.direction &&
      actual.scriptName == expected.scriptName &&
      actual.scriptType == expected.scriptType &&
      actual.scriptGitUrl == expected.scriptGitUrl &&
      actual.scriptHash == expected.scriptHash &&
      actual.scriptVersionId == expected.scriptVersionId &&
      actual.tableName == expected.tableName &&
      actual.tableSchema == expected.tableSchema &&
      actual.tablePlatform == expected.tablePlatform
    }

    queriedJoinedRows.foreach { actual =>
      assert(expectedJoinedEntities.exists(expected => compareJoin(actual, expected)))
    }
  }
}
