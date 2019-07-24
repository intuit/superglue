package com.intuit.superglue.lineage.model

import com.intuit.superglue.dao.model.PrimaryKeys.TablePK

sealed trait Node {
  def id: Long,
}

object Node {
  case class TableNode(pk: TablePK, name: String, group: String = "table") extends Node {
    override def id: Long = pk.value
  }
}
