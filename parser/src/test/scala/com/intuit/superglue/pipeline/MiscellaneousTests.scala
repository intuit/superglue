package com.intuit.superglue.pipeline

import com.intuit.superglue.pipeline.Metadata.StatementMetadataFragment
import com.intuit.superglue.pipeline.parsers.NopStatementParser
import org.scalatest.FlatSpec

class MiscellaneousTests extends FlatSpec {

  "The StringEtc trait" should "provide an etc extension method to shorten a string" in {
    import Implicits.StringEtc
    assert("The quick brown fox jumped over the lazy dog".etc(22).equals("The quick brown fox..."))
  }

  "The NopStatementParser" should "return a dummy metadata object" in {
    val metadata = NopStatementParser.parseStatement("SELECT * FROM table")
    assert(metadata ==
      StatementMetadataFragment(
        statementParser = "com.intuit.superglue.pipeline.parsers.NopStatementParser$",
        statementType = "",
        inputObjects = List.empty[String],
        outputObjects = List.empty[String],
        List.empty[Throwable],
      )
    )
  }
}
