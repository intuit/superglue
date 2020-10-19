package com.intuit.superglue.pipeline

import java.nio.file.Files

import com.intuit.superglue.pipeline.producers.{ScriptFileProvider, ScriptProvider}

import scala.collection.JavaConverters._

/**
  * Tests the [[ScriptFileProvider]] of the parser.
  *
  * # Test setup
  * First, instantiate a Fixture with the names of files that should exist
  * in the virtual filesystem during this test. The fixture will create all
  * the parent directories on the way to the file.
  * Then, create a [[FileInputConfig]] that represents a group of files you'd
  * like the FileProvider to find. Then manually write out the list of files
  * you expect it to find, and compare the two.
  */
class ScriptFileProviderTest extends FsSpec {

  "A virtual filesystem" should "have the defined files" in {
    val f = Fixture(List(
      "myScript1.sql",
      "/one/two/myScript2.sql",
      "myLauncher.sh",
      "yourUnrelated.sql",
    ))

    // Each given file should appear in the filesystem
    val foundFiles = Files.walk(f.root).iterator().asScala.toList
    assert(f.filenames.forall { given =>
      foundFiles.map(_.toString).exists { found =>
        found.endsWith(given)
      }
    })
  }

  "A FileProvider" should "include globbed files in a single directory" in {
    val f = Fixture(List(
      "one/myScript1.sql",
      "one/myScript2.sql",
      "two/myScript3.sql",
      "two/myScript4.sql",
    ))

    val config = FileInputConfig(
      includes = List("glob:/one/*.sql"),
      base = "/",
    )
    val provider = new ScriptFileProvider(config)(f.fs)

    val expected = List(
      "one/myScript1.sql",
      "one/myScript2.sql",
    )
    val actual = provider.stream().map(_.name).toList
    assert(actual == expected)
  }

  it should "exclude globbed files in a single directory" in {
    val f = Fixture(List(
      "/test/wantScript1.sql",
      "/test/wantScript2.sql",
      "/test/dontScript3.sql",
      "/test/dontScript4.sql",
    ))

    val config = FileInputConfig(
      excludes = List("glob:/test/dont*.sql"),
      base = "/",
    )
    val provider = new ScriptFileProvider(config)(f.fs)

    val expected = List(
      "test/wantScript1.sql",
      "test/wantScript2.sql",
    )
    val actual = provider.stream().map(_.name).toList
    assert(actual == expected)
  }

  it should "include and exclude globbed files, with exclude priority" in {
    val f = Fixture(List(
      "/test/mehScript1.sql",
      "/test/mehScript2.sql",
      "/test/includeScript1.sql",
      "/test/includeScript2.sql",
      "/test/includeScriptActuallyDont.sql",
    ))

    val config = FileInputConfig(
      includes = List("glob:/test/include*.sql"),
      excludes = List("glob:/test/*Dont*"),
      base = "/",
    )
    val provider = new ScriptFileProvider(config)(f.fs)

    val expected = List(
      "test/includeScript1.sql",
      "test/includeScript2.sql",
    )
    val actual = provider.stream().map(_.name).toList
    assert(actual == expected)
  }

  it should "include globbed files across multiple directories" in {
    val f = Fixture(List(
      "/yes/one/includeScript1.sql",
      "/yes/one/mehScript2.sql",
      "/yes/two/includeScript3.sql",
      "/yes/two/mehScript4.sql",
      "/yes/two/includeScriptActuallyDont.sql",
      "/no/includeScript1.sql",
      "/no/mehScript2.sql",
    ))

    val config = FileInputConfig(
      includes = List("glob:/yes/**/*include*"),
      excludes = List("glob:**/*Dont*"),
      base = "/",
    )
    val provider = new ScriptFileProvider(config)(f.fs)

    val expected = List(
      "yes/one/includeScript1.sql",
      "yes/two/includeScript3.sql",
    )
    val actual = provider.stream().map(_.name).toList
    assert(actual == expected)
  }

  it should "include and exclude files with a regex pattern" in {
    val f = Fixture(List(
      "/test/script10.sql",
      "/test/script20.sql",
      "/test/script100.sql",
      "/test/script200.sql",
      "/test/script1000.sql",
      "/test/script2000.sql",
    ))

    val config = FileInputConfig(
      includes = List("regex:/test/script\\d{3}\\.sql"),
      base = "/",
    )
    val provider = new ScriptFileProvider(config)(f.fs)
    val expected = List(
      "test/script100.sql",
      "test/script200.sql",
    )
    val actual = provider.stream().map(_.name).toList
    assert(actual == expected)
  }

  it should "include and exclude literal file paths with no globbing" in {
    val f = Fixture(List(
      "/test/mehScript1.sql",
      "/test/mehScript2.sql",
      "/test/includeScript1.sql",
      "/test/includeScript2.sql",
      "/test/includeScriptActuallyDont.sql",
    ))

    val config = FileInputConfig(
      includes = List("/test/includeScript1.sql", "/test/includeScript2.sql"),
      excludes = List("/test/includeScript1.sql"),
      base = "/",
    )
    val provider = new ScriptFileProvider(config)(f.fs)
    val expected = List("test/includeScript2.sql")
    val actual = provider.stream().map(_.name).toList
    assert(actual == expected)
  }

  it should "annotate the inputKind and inputDialect of each input" in {
    val f = Fixture(List(
      "/test/script1.sql",
      "/test/script2.hql",
      "/test/script3.sh",
    ))

    val sqlConfig = FileInputConfig(
      includes = List("glob:**.sql"),
      kind = Some("sql"),
      dialect = Some("vertica"),
      base = "/",
    )
    val hqlConfig = FileInputConfig(
      includes = List("glob:**.hql"),
      dialect = Some("hive"),
      base = "/",
    )
    val otherConfig = FileInputConfig(
      excludes = List("glob:**.sql", "glob:**.hql"),
      base = "/",
    )
    val sqlProvider = new ScriptFileProvider(sqlConfig)(f.fs)
    val hqlProvider = new ScriptFileProvider(hqlConfig)(f.fs)
    val otherProvider = new ScriptFileProvider(otherConfig)(f.fs)

    val sqlExpected = List(("test/script1.sql", "sql", Some("vertica")))
    val hqlExpected = List(("test/script2.hql", "sql_hive", Some("hive")))
    val otherExpected = List.empty[(String, String, Option[String])]

    def fn(p: ScriptProvider) = p.stream().map(i => Tuple3(i.name, i.kind, i.dialect)).toList
    val sqlActual = fn(sqlProvider)
    val hqlActual = fn(hqlProvider)
    val otherActual = fn(otherProvider)

    assert(sqlActual == sqlExpected)
    assert(hqlActual == hqlExpected)
    assert(otherActual == otherExpected)
  }

  it should "include files using relative paths" in {
    val f = Fixture(List(
      "/Users/testUser/Documents/repository/sql/scriptA.sql",
      "/Users/testUser/Documents/repository/sql/scriptB.sql",
      "/Users/testUser/Documents/repository/sql/scriptC.sql",
      "/Users/testUser/Documents/repository/root.sql",
      "/Users/testUser/Documents/repository/java/Main.java",
      "/Users/testUser/Downloads/hello.jpg",
      "/Library/macStuff/important.darwin",
    ))

    val txtConfig = FileInputConfig(
      includes = List("glob:**.sql"),
      kind = Some("sql"),
      base = "/Users/testUser/Documents/repository",
    )
    val txtProvider = new ScriptFileProvider(txtConfig)(f.fs)
    val files = txtProvider.stream().toList
    val expected = List(
      "sql/scriptA.sql",
      "sql/scriptB.sql",
      "sql/scriptC.sql",
      "root.sql",
    )

    assert(expected.forall(path => files.exists(f => f.name == path)))
  }
}
