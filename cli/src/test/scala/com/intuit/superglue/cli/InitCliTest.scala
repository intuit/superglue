package com.intuit.superglue.cli

import java.nio.file.Files

class InitCliTest extends CliSpec {

  private def inMemoryDbConfig(dbName: String): String =
    s"""
       |com.intuit.superglue.dao {
       |  backend = "relational"
       |  relational {
       |    profile = "slick.jdbc.H2Profile$$"
       |    dataSourceClass = "slick.jdbc.DatabaseUrlDataSource"
       |    numThreads = 1
       |    db {
       |      driver = "org.h2.Driver"
       |      url = "jdbc:h2:mem:$dbName"
       |      user = ""
       |      password = ""
       |    }
       |  }
       |}
    """.stripMargin

  it should "print when the db configuration is not correct" in Fixture { f =>
    val confFile = f.root.resolve("/application.conf")
    Files.createFile(confFile)
    val conf = ""
    Files.write(confFile, conf.getBytes)

    val args = Array("init", "--database", "-c", "/application.conf")
    f.main.run(args)
    assert(f.stdoutString.trim.contains("Warning: Given configuration does not have 'com.intuit.superglue.dao'"))
    assert(f.stdoutString.trim.contains("Timed out while attempting to connect to Superglue"))
  }

  it should "Print when a config file was not found" in Fixture { f =>
    val args = Array("init", "--database", "-c", "/nonexistent/application.conf")
    f.main.run(args)
    assert(f.stdoutString.trim.contains("Configuration file '/nonexistent/application.conf' not found. Using default configuration"))
  }

  it should "Print usage when given no arguments" in Fixture { f =>
    val args = Array("init")
    f.main.run(args)
    assert(f.stdoutString.trim.contains("Usage: superglue init"))
  }
}
