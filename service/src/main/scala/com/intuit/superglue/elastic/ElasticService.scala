package com.intuit.superglue.elastic

import com.intuit.superglue.dao.model.TableEntity
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.index.CreateIndexResponse
import com.sksamuel.elastic4s.http.index.admin.AliasActionResponse
import com.sksamuel.elastic4s.http.index.alias.Alias
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties, Response}
import com.sksamuel.elastic4s.{Index, RefreshPolicy}
import com.typesafe.config.{Config => TypesafeConfig}
import io.tmos.arm.ArmMethods._
import io.tmos.arm.CanManage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class ElasticService(rootConfig: TypesafeConfig) {
  private val elasticConfig = ElasticsearchConfig(rootConfig.getConfig("com.intuit.superglue.elastic")).get

  // Implementation of the CanManage typeclass for ElasticClient.
  // This allows us to use the "manage" function from Automatic Resource Manager (ARM)
  implicit val canManageElasticClient: CanManage[ElasticClient] = new CanManage[ElasticClient] {
    override def onFinally(client: ElasticClient): Unit = client.close()
  }

  /**
    * Performs a series of operations within one session of an Elasticsearch Client.
    *
    * Once the session is exited, the connection to elasticsearch is closed. Due to
    * this, try to minimize the number of times the "perform" function is called, but
    * maximize the number of operations done within a single session. For example:
    *
    * Fetch tables from the data layer and upload them to elasticsearch:
    * {{{
    *   val futureResponse = perform { superglueRepository => implicit esClient =>
    *     for {
    *       tables <- superglueRepository.tableRepository.getAll
    *       uploadResponse <- uploadTables(tables)
    *     } yield uploadResponse
    *   }
    *   // Remember to await on the result or do something else with the future!
    *   val response = Await.result(futureResponse, 10 second)
    * }}}
    *
    * @param session A function which is given access to the superglue repository and
    *                the elasticsearch client. Operations requiring these resources
    *                should be performed in the scope of this call.
    * @tparam R The type of the value returned when execution completes.
    * @return A Future - the value returned by the session function.
    */
  def perform[R](session: ElasticClient => R): R = for {
    client <- manage(ElasticClient(ElasticProperties(s"${elasticConfig.hostname}:${elasticConfig.port}")))
  } yield session(client)

  /**
    * Creates the index if it does not already exist on the
    * connected ElasticSearch cluster. This ensures we have the proper mappings
    * which help completion suggestion to work.
    *
    * @param client A handle to an open ElasticSearch HttpClient.
    */
  def createIndexIfNotExists(implicit client: ElasticClient): Future[Option[Response[CreateIndexResponse]]] = {
    for {
      // Check whether the index exists
      exists <- client.execute {
        typesExist(elasticConfig.index / elasticConfig.indexType)
      }.map(_.result.exists)

      // If the index does not exist, create it and return the response. Otherwise return None.
      response <- if (exists) Future.successful(None)
                  else client.execute {
        createIndex(elasticConfig.index)
          .settings(Map(
            "analysis" -> Map(
              "tokenizer" -> Map(
                "autocomplete_tokenizer" -> Map(
                  "type" -> "edge_ngram",
                  "min-gram" -> 2,
                  "max_gram" -> 10,
                  "token_chars" -> Seq("letter", "digit")
                )
              ),
              "analyzer" -> Map(
                "autocomplete" -> Map(
                  "type" -> "custom",
                  "tokenizer" -> "autocomplete_tokenizer",
                  "filter" -> Seq("lowercase")
                )
              )
            )
          ))
          .mappings(
            mapping(elasticConfig.indexType).fields(
              textField("name").analyzer("autocomplete"),
              keywordField("type"),
              textField("schema"),
              textField("platform")
            )
          )
      }.map(Some(_))
    } yield response
  }

  /**
    * In order to keep 100% uptime on our lineage index, we use an alias
    * to redirect requests to the current "production" index.
    *
    * For example, if our alias is called "lineage", then our clients will
    * connect to the search index through the name "lineage" always. However,
    * at any given time between deployments and remappings, we may use
    * different underlying indices to store the documents, such as "lineage1",
    * "lineage2", etc. When we make a remapping to a new index, we simply
    * rewrite the alias to point to the new production index.
    *
    * @param client An open ElasticSearch http client.
    * @return True if the alias was successfully written as
    *         "indexAlias" -> "indexName", false otherwise.
    */
  def createIndexAlias(implicit client: ElasticClient): Future[Response[AliasActionResponse]] = {
    for {
      getAliasesRequest <- client.execute(getAliases())

      // Delete each alias pointing from "indexAlias" to any index.
      deleteAliasesRequests = getAliasesRequest.result.mappings
        .collect {
          case (Index(index), aliases) if aliases.contains(Alias(elasticConfig.alias)) =>
            client.execute(removeAlias(elasticConfig.alias, index))
        }
      _ <- Future.sequence(deleteAliasesRequests)

      // Create a new alias from "indexName" to "indexAlias"
      createAliasRequest <- client.execute {
        addAlias(elasticConfig.alias, elasticConfig.index)
      }
    } yield createAliasRequest
  }

  /**
    * Given a set of table entities, inserts them as documents into elasticsearch to be searched.
    *
    * @param tables The tables to upload to elasticsearch.
    * @return The responses to the elasticsearch bulk inserts as a sequence of futures
    */
  def uploadTables(tables: Seq[TableEntity])(implicit client: ElasticClient): Seq[Future[Response[BulkResponse]]] = {
    tables
      .grouped(elasticConfig.batchSize)
      .map { batchTables =>
        client.execute {
          bulk(
            batchTables.map { table =>
              indexInto(elasticConfig.index / elasticConfig.indexType)
                .id(table.id.value.toString)
                .fields(
                  "name" -> table.name,
                  "type" -> "TABLE",
                  "schema" -> table.schema,
                  "platform" -> table.platform,
                )
            }
          ).refresh(RefreshPolicy.WaitFor)
        }
      }.toSeq
  }
}
