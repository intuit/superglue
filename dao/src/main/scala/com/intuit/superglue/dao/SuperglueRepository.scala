package com.intuit.superglue.dao

import com.intuit.superglue.dao.relational.SuperglueRelationalRepository
import com.typesafe.config.{Config => TypesafeConfig}
import slick.basic.DatabaseConfig

import scala.concurrent.Future

object SuperglueRepository {

  /**
    * The factory method for instantiating a SuperglueRepository.
    *
    * The factory will select a repository implementation based on the
    * configuration given.
    *
    * @param rootConfig The top-level configuration object for this application.
    *                   The SuperglueRepository reads values from the
    *                   {{{com.intuit.superglue.dao}}} namespace of the config.
    * @return {{{Some(SuperglueRepository)}}} if the configuration is valid, or
    *        {{{None}}} if it is not.
    */
  def apply(rootConfig: TypesafeConfig): Option[SuperglueRepository] = {
    val daoConfig = rootConfig.getConfig("com.intuit.superglue.dao")
    daoConfig.getString("backend") match {
      case "relational" => Some(new SuperglueRelationalRepository(DatabaseConfig.forConfig("relational", daoConfig)))
      case _ => None
    }
  }
}

/**
  * The primary interface for interacting with Superglue data.
  *
  * Each element in Superglue's data model is represented by a repository,
  * which is an interface for inserting or querying entities. Different
  * instances of repositories can be backed by different persistence layers,
  * e.g. a relational database vs a graph database.
  */
trait SuperglueRepository {
  def initialize(testMode: Boolean = false): Future[Unit]
  val tableRepository: TableRepository
  val scriptRepository: ScriptRepository
  val statementRepository: StatementRepository
  val scriptTableRepository: ScriptTableRepository
  val statementTableRepository: StatementTableRepository
  val lineageViewRepository: LineageViewRepository
}
