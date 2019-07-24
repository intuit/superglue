package com.intuit.superglue.dao.model

object PrimaryKeys {
  import slick.lifted.MappedTo

  /** A typed wrapper around a Long to indicate the primary key of a Table */
  case class TablePK(value: Long) extends AnyVal with MappedTo[Long]

  /** A typed wrapper around a Long to indicate the primary key of a Script */
  case class ScriptPK(value: Long) extends AnyVal with MappedTo[Long]

  /** A typed wrapper around a Long to indicate the primary key of a Statement */
  case class StatementPK(value: Long) extends AnyVal with MappedTo[Long]

  /** A typed wrapper around a Long to indicate the primary key of a ScriptTableRelation */
  case class ScriptTablePK(value: Long) extends AnyVal with MappedTo[Long]

  /** A typed wrapper around a Long to indicate the primary key of a StatementTableRelation */
  case class StatementTablePK(value: Long) extends AnyVal with MappedTo[Long]
}
