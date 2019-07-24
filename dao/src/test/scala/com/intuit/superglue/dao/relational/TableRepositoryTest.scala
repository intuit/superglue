package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model.TableEntity

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class TableRepositoryTest extends InMemoryDataSpec {

  "A Table Repository" should "store and retrieve table entities one at a time" in Fixture { f =>
    val tablesRepo = f.sgRepo.tableRepository

    val tableOne = TableEntity(name = "table_one", schema = "qb_stable", platform = "VERTICA")
    val tableTwo = TableEntity(name = "table_two", schema = "qb_sandbox", platform = "VERTICA")

    val insertAction = for {
      insertOneCount <- tablesRepo.add(tableOne)
      insertTwoCount <- tablesRepo.add(tableTwo)
    } yield insertOneCount + insertTwoCount

    // Insert the entities
    val count = Await.result(insertAction, 1 second)
    assert(count == 2)

    val queryAction = for {
      queryOne <- tablesRepo.getByName("table_one")
      queryTwo <- tablesRepo.getByName("table_two")
    } yield queryOne ++ queryTwo

    // Get the entities back from storage
    val storedTables = Await.result(queryAction, 1 second)

    assert(storedTables == Seq(tableOne, tableTwo))
  }

  it should "store table entities many at a time" in Fixture { f =>
    val tablesRepo = f.sgRepo.tableRepository

    val tableOne = TableEntity(name = "table_one", schema = "qb_stable", platform = "VERTICA")
    val tableTwo = TableEntity(name = "table_two", schema = "qb_sandbox", platform = "VERTICA")

    val entities = Set(tableOne, tableTwo)
    val insertedTableCounts = Await.result(tablesRepo.addAll(entities), 1 second)
    assert(insertedTableCounts == 2)

    val storedTables = Await.result(tablesRepo.getAll, 1 second)
    assert(storedTables.size == insertedTableCounts)

    val expected = Set(tableOne, tableTwo)
    storedTables.foreach { actual =>
      assert(expected.contains(actual))
    }
  }

  it should "handle duplicates gracefully with upserts" in Fixture { f =>
    val tablesRepo = f.sgRepo.tableRepository

    val theTable = TableEntity("table_name", "schema", "platform")
    val action = for {
      one <- tablesRepo.add(theTable)
      two <- tablesRepo.add(theTable)
    } yield one + two

    val insertCount = Await.result(action, 1 second)
    assert(insertCount == 2)
  }

  it should "handle batch inserts gracefully with upserts" in Fixture { f =>
    val tablesRepo = f.sgRepo.tableRepository

    val theTable = TableEntity("table_name", "schema", "platform")
    val theTables = Set(theTable, theTable)

    val action = tablesRepo.addAll(theTables)
    val insertCount = Await.result(action, 1 second)

    // Batch inserts de-duplicate entities, so we end up with 1 insert.
    assert(insertCount == 1)
  }

  it should "find tables by their name" in Fixture { f =>
    val tablesRepo = f.sgRepo.tableRepository

    val tables = Seq(
      TableEntity("apple", "production", "platform"),
      TableEntity("banana", "production", "platform"),
      TableEntity("carrot", "test", "platform"),
      TableEntity("apple", "test", "platform"),
      TableEntity("eggplant", "development", "platform"),
      TableEntity("fruit", "test", "platform"),
      TableEntity("apple", "development", "platform"),
    )

    val action = tablesRepo.addAll(tables.toSet)
    val count = Await.result(action, 1 second)
    assert(count == 7)

    val getApplesAction = tablesRepo.getByName("apple")
    val apples = Await.result(getApplesAction, 1 second)
    assert(apples.size == 3)
    assert(apples.toSet == Set(
      TableEntity("apple", "production", "platform"),
      TableEntity("apple", "test", "platform"),
      TableEntity("apple", "development", "platform"),
    ))
  }
}
