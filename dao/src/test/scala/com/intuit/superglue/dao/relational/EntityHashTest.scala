package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model.TableEntity
import org.scalatest.FlatSpec

class EntityHashTest extends FlatSpec {

  "An entity" should "have the same hash for the same contents" in {
    val tableOne = TableEntity("name", "schema", "platform")
    val tableTwo = TableEntity("name", "schema", "platform")
    assert(tableOne.id == tableTwo.id)
  }

  it should "have a different hash for different contents" in {
    val tableOne = TableEntity("abcdefg", "schema", "platform")
    val tableTwo = TableEntity("zyxwvut", "schema", "platform")
    assert(tableOne.id != tableTwo.id)
  }
}
