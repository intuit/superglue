package com.intuit.superglue.pipeline.parsers

import java.util

object SimpleStatementSplitter extends StatementSplitter {
  private val DELIMITER: Char = ';'

  /**
    * Splits a SQL script on unquoted semicolons.
    *
    * @param body The whole body of a script.
    * @return A collection of the individual statements in the script.
    */
  override def splitStatements(body: String): util.Collection[String] = {
    val statements = new util.ArrayList[String]()
    var startIndexOfUnquoted = 0
    var inQuotes = false
    for ((ch, currentIndex) <- body.toCharArray.iterator.zipWithIndex) {
      // We're at the end of the string if the index is length-1
      val atEnd = currentIndex == body.length - 1
      if (ch == '\'') inQuotes = !inQuotes

      if (atEnd) {
        // When we reach the end of the script, add the last unquoted string as a statement
        val end = if (ch == DELIMITER) { currentIndex } else { currentIndex + 1 }
        val stmt = body.substring(startIndexOfUnquoted, end).trim
        if (!"".equals(stmt)) statements.add(stmt)
        return statements
      }
      // When we see an unquoted semicolon, that's the end of a statement
      if (ch == DELIMITER && !inQuotes) {
        val stmt = body.substring(startIndexOfUnquoted, currentIndex).trim
        if (!"".equals(stmt)) statements.add(stmt)
        startIndexOfUnquoted = currentIndex + 1
      }
    }
    statements
  }
}
