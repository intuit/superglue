package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model.PrimaryKeys.ScriptPK
import com.intuit.superglue.dao.model.StatementEntity

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class StatementRepositoryTest extends InMemoryDataSpec {

  "A StatementRepository" should "insert and retrieve statement entities one at a time" in Fixture { f =>
    val statementOne = StatementEntity("SELECT", "SELECT * FROM my_table", ScriptPK(10))
    val statementTwo = StatementEntity("INSERT", "INSERT INTO table_two VALUES ('hello')", ScriptPK(10))

    val insertAction = for {
      insertOneCount <- f.statementRepo.add(statementOne)
      insertTwoCount <- f.statementRepo.add(statementTwo)
    } yield insertOneCount + insertTwoCount

    val insertCount = Await.result(insertAction, 1 second)
    assert(insertCount == 2)

    val queryAction = for {
      queryOne <- f.statementRepo.getByType("SELECT")
      queryTwo <- f.statementRepo.getByType("INSERT")
    } yield queryOne ++ queryTwo

    val allStatements = Await.result(queryAction, 1 second)
    assert(allStatements.size == 2)
    assert(allStatements.contains(statementOne))
    assert(allStatements.contains(statementTwo))
  }

  it should "store statements many at a time" in Fixture { f =>
    val statementOne = StatementEntity("SELECT", "SELECT * FROM my_table", ScriptPK(10))
    val statementTwo = StatementEntity("INSERT", "INSERT INTO table_two VALUES ('hello')", ScriptPK(10))

    val insertCount = Await.result(f.statementRepo.addAll(Set(statementOne, statementTwo)), 1 second)
    assert(insertCount == 2)

    val queried = Await.result(f.statementRepo.getAll, 1 second)
    assert(queried.size == 2)
    assert(queried.contains(statementOne))
    assert(queried.contains(statementTwo))
  }

  it should "handle duplicates gracefully with upserts" in Fixture { f =>
    val statement = StatementEntity("SELECT", "select one, two from the_Table", ScriptPK(0))

    val insertAction = for {
      insertOne <- f.statementRepo.add(statement)
      insertTwo <- f.statementRepo.add(statement)
    } yield insertOne + insertTwo

    val insertCount = Await.result(insertAction, 1 second)
    assert(insertCount == 2)

    val queried = Await.result(f.statementRepo.getAll, 1 second)
    assert(queried.size == 1)
    assert(queried.contains(statement))
  }
}
