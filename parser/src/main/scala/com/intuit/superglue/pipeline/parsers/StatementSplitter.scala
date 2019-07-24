package com.intuit.superglue.pipeline.parsers

import java.util

/**
  * A statement splitter takes in the whole body of a script and returns
  * a list of the individual statements.
  */
trait StatementSplitter {
  /**
    * Takes the whole body of a (perhaps preprocessed) script and splits it
    * into a collection of its individual statements.
    * @param body The whole body of a script.
    * @return A collection of the individual statements in the script.
    */
  def splitStatements(body: String): util.Collection[String]
}
