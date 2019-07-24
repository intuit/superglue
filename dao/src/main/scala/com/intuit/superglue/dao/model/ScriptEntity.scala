package com.intuit.superglue.dao.model

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.intuit.superglue.dao.model.PrimaryKeys._

/**
  * Represents a single script whose lineage is tracked by Superglue.
  *
  * A ScriptEntity is required to be populated with its own attributes,
  * and may optionally store references to [[TableEntity]]s that it's
  * associated with. An example usage is shown below:
  *
  * {{{
  *   // Minimum information required to instantiate a ScriptEntity
  *   val scriptEntity = ScriptEntity(
  *     "myScript.sql",
  *     "SQL",
  *     "https://github.com/org/repo",
  *     "abcdefghijklmnopqrstuvwxyz")
  * }}}
  *
  * When creating a new [[ScriptEntity]] from data (e.g. when parsing a script),
  * the "id" field should be omitted, and it will automatically be generated
  * as a hash of the [[ScriptEntity]] contents.
  *
  * @param name The name of the script
  * @param scriptType The type of script (e.g. "SQL")
  * @param scriptGitUrl The URL to the git repository where this script is stored
  * @param scriptHash A hash of the script's contents
  * @param scriptVersionId An ID to represent the same script that's been updated
  * @param id A unique key generated from the [[ScriptEntity]] contents
  * @param tables An optional set of relationships to [[TableEntity]]s
  */
case class ScriptEntity(
  name: String,
  scriptType: String,
  scriptGitUrl: String,
  scriptHash: String,
  scriptVersionId: Int,
  id: ScriptPK,
  tables: Set[(Direction, Set[TableEntity])],
)

object ScriptEntity {
  def apply(
    name: String,
    scriptType: String,
    scriptGitUrl: String,
    scriptHash: String,
    scriptVersionId: Int = 0,
    tables: Set[(Direction, Set[TableEntity])] = Set.empty,
  ): ScriptEntity = {
    val normalizedType = scriptType.toUpperCase

    val id = Hashing.sipHash24().newHasher()
      .putString(name, Charsets.UTF_8)
      .putString(normalizedType, Charsets.UTF_8)
      .putString(scriptGitUrl, Charsets.UTF_8)
      .putString(scriptHash, Charsets.UTF_8)
      .putInt(scriptVersionId)
      .hash().asLong()
    ScriptEntity(
      name,
      normalizedType,
      scriptGitUrl,
      scriptHash,
      scriptVersionId,
      ScriptPK(id),
      tables,
    )
  }
}
