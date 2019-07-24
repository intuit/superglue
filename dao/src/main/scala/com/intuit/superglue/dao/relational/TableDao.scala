package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model.TableEntity
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * When implemented for a type that has a JDBC profile, this trait
  * provides implementations of relational queries for [[TableEntity]]s.
  */
trait TableDao { self: Profile =>
  import profile.api._

  /**
    * A mapping from [[TableEntity]] onto a relational database table.
    */
  class TableDef(tag: Tag) extends Table[TableEntity](tag, "TABLES") {
    def id = column[TablePK]("table_id", O.PrimaryKey)
    def name = column[String]("table_name", O.Length(400))
    def schema = column[String]("table_schema", O.Length(400))
    def platform = column[String]("table_platform", O.Length(400))

    /**
      * Converts a row from a relational database into a [[TableEntity]].
      */
    def intoTable(data: (String, String, String, TablePK)): TableEntity = {
      val (name, schema, platform, id) = data
      TableEntity(name, schema, platform, id)
    }

    /**
      * Defines the full projection of the relational database table.
      */
    override def * = (name, schema, platform, id) <> (intoTable, TableEntity.unapply)
  }

  lazy val tablesQuery = TableQuery[TableDef]

  def getAllTablesQuery = tablesQuery

  def getTablesByNameQuery(name: String) = tablesQuery.filter(_.name === name)

  def upsertTableQuery(tableEntity: TableEntity) =
    tablesQuery insertOrUpdate tableEntity

  def upsertAllTablesQuery(tableEntities: Set[TableEntity]) =
    DBIO.sequence(tableEntities.toSeq.map(upsertTableQuery))
}
