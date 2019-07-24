package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model.ScriptEntity

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.language.postfixOps

class ScriptRepositoryTest extends InMemoryDataSpec {

  "A Script Repository" should "store and retrieve script entities one at a time" in Fixture { f =>
    val scriptOne = ScriptEntity(
      name = "qbo_company_status_etl",
      scriptType = "sql",
      scriptGitUrl = "https://github.intuit.com/sbg-data-mart/sbg_stable_analyst_scripts/",
      scriptHash = "###",
      scriptVersionId = 1,
    )

    val scriptTwo = ScriptEntity(
      name = "qbo_company_status",
      scriptType = "sql",
      scriptGitUrl = "https://github.intuit.com/sbg-data-mart/sbg_stable_analyst_scripts/",
      scriptHash = "###",
      scriptVersionId = 1,
    )

    val insertAction = for {
      insertOneCount <- f.scriptRepo.add(scriptOne)
      insertTwoCount <- f.scriptRepo.add(scriptTwo)
    } yield insertOneCount + insertTwoCount

    // Insert the entities, getting the updated entities (with set primary keys) back.
    val insertedScriptsCount = Await.result(insertAction, 1 second)
    assert(insertedScriptsCount == 2)

    val queryAction = for {
      queryOne <- f.scriptRepo.getByName("qbo_company_status_etl")
      queryTwo <- f.scriptRepo.getByName("qbo_company_status")
    } yield queryOne ++ queryTwo

    // Get the entities back from storage
    val storedScripts = Await.result(queryAction, 1 second)

    val expected = Seq(scriptOne, scriptTwo)
    storedScripts.foreach { actual =>
      assert(expected.contains(actual))
    }
  }

  it should "store script entities many at a time" in Fixture { f =>
    val scriptOne = ScriptEntity(
      name = "qbo_company_status_etl",
      scriptType = "sql",
      scriptGitUrl = "https://github.intuit.com/sbg-data-mart/sbg_stable_analyst_scripts/",
      scriptHash = "###",
      scriptVersionId = 1,
    )

    val scriptTwo = ScriptEntity(
      name = "qbo_company_status",
      scriptType = "sql",
      scriptGitUrl = "https://github.intuit.com/sbg-data-mart/sbg_stable_analyst_scripts/",
      scriptHash = "###",
      scriptVersionId = 1,
    )

    val entities = Set(scriptOne, scriptTwo)
    val insertedScriptCounts = Await.result(f.scriptRepo.addAll(entities), 1 second)
    assert(insertedScriptCounts == 2)

    val storedScripts = Await.result(f.scriptRepo.getAll, 1 second)
    val expected = Set(scriptOne, scriptTwo)

    storedScripts.foreach { actual =>
      assert(expected.contains(actual))
    }
  }

  it should "handle duplicates gracefully with upserts to avoid constraint violations" in Fixture { f =>
    val theScript = ScriptEntity(
      name = "qbo_company_status_etl",
      scriptType = "sql",
      scriptGitUrl = "https://github.intuit.com/sbg-data-mart/sbg_stable_analyst_scripts/",
      scriptHash = "###",
      scriptVersionId = 1,
    )

    val action = for {
      one <- f.scriptRepo.add(theScript)
      two <- f.scriptRepo.add(theScript)
    } yield one + two

    val insertedCount = Await.result(action, 1 second)
    assert(insertedCount == 2)
  }

  it should "handle duplicates in batches gracefully" in Fixture { f =>
    val theScript = ScriptEntity(
      name = "qbo_company_status_etl",
      scriptType = "sql",
      scriptGitUrl = "https://github.intuit.com/sbg-data-mart/sbg_stable_analyst_scripts/",
      scriptHash = "###",
      scriptVersionId = 1,
    )

    val toInsert = Set(theScript, theScript)
    val action = f.scriptRepo.addAll(toInsert)
    val insertedCount = Await.result(action, 1 second)

    // Batch insert de-duplicates entities, so we end up with 1 insert.
    assert(insertedCount == 1)
  }

  it should "find scripts by their name" in Fixture { f =>
    val scripts = Seq(
      ScriptEntity("antelope", "SQL", "git://", "####"),
      ScriptEntity("beaver", "sql", "svn://", "####"),
      ScriptEntity("catfish", "SHELL", "git://", "####"),
      ScriptEntity("dragonfly", "SQL", "svn://", "####"),
      ScriptEntity("beaver", "PYTHON", "git://", "####"),
    )

    val insertAction = f.scriptRepo.addAll(scripts.toSet)
    val count = Await.result(insertAction, 1 second)
    assert(count == 5)

    val getBeaversAction = f.scriptRepo.getByName("beaver")
    val beavers = Await.result(getBeaversAction, 1 second)
    assert(beavers.size == 2)
    assert(beavers.toSet == Set(
      ScriptEntity("beaver", "SQL", "svn://", "####"),
      ScriptEntity("beaver", "PYTHON", "git://", "####"),
    ))
  }
}
