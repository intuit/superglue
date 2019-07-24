package com.intuit.superglue.dao.relational

import com.intuit.superglue.dao.model._
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * When implemented for a type that has a JDBC profile, this trait
  * provides implementations of relational queries for [[ScriptEntity]]s.
  */
trait ScriptDao { self: Profile =>
  import profile.api._

  /**
    * A mapping from [[ScriptEntity]] onto a relational database table.
    */
  class ScriptDef(tag: Tag) extends Table[ScriptEntity](tag, "SCRIPTS") {
    def id = column[ScriptPK]("script_id", O.PrimaryKey)
    def name = column[String]("script_name", O.Length(400))
    def scriptType = column[String]("script_type", O.Length(400))
    def scriptGitUrl = column[String]("script_giturl", O.Length(400))
    def scriptHash = column[String]("script_hash", O.Length(400))
    def scriptVersionId = column[Int]("script_version_id")

    /**
      * Converts a row from a relational database into a [[ScriptEntity]].
      */
    def intoScript(tuple: (String, String, String, String, Int, ScriptPK)): ScriptEntity = {
      val (name, scriptType, gitUrl, hash, versionId, id) = tuple
      ScriptEntity(name, scriptType, gitUrl, hash, versionId, id, Set.empty)
    }

    /**
      * Converts a [[ScriptEntity]] into a row for a relational database.
      */
    def fromScript(scriptEntity: ScriptEntity): Option[(String, String, String, String, Int, ScriptPK)] = {
      Some((scriptEntity.name,
        scriptEntity.scriptType,
        scriptEntity.scriptGitUrl,
        scriptEntity.scriptHash,
        scriptEntity.scriptVersionId,
        scriptEntity.id))
    }

    /**
      * Defines the full projection of the relational database table.
      */
    override def * = (name, scriptType, scriptGitUrl, scriptHash, scriptVersionId, id) <> (intoScript, fromScript)
  }

  lazy val scriptsQuery = TableQuery[ScriptDef]

  def getAllScriptsQuery = scriptsQuery

  def getScriptsByNameQuery(name: String) = scriptsQuery.filter(_.name === name)

  def upsertScriptQuery(scriptEntity: ScriptEntity) =
    scriptsQuery insertOrUpdate scriptEntity

  def upsertAllScriptsQuery(scriptEntities: Set[ScriptEntity]) = {
    DBIO.sequence(scriptEntities.toSeq.map(upsertScriptQuery))
  }
}
