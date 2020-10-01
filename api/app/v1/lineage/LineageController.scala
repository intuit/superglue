package v1.lineage

import com.intuit.superglue.lineage.model.Graph
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * This controller serves all endpoints prefixed with {{{/v1/lineage/}}}.
  *
  * @param cc All of the resources required for serving the endpoints in this controller.
  * @param ec The execution context (i.e. thread executor) on which to run asynchronous actions.
  */
class LineageController @Inject()(cc: LineageControllerComponents)
  (implicit ec: ExecutionContext)
  extends LineageBaseController(cc) {

  /**
    * The handler for the endpoint {{{GET /v1/lineage/table/:name}}}.
    *
    * This endpoint searches for table lineage centered on the given table name.
    * The response is a JSON payload with a subgraph of the table lineage specified
    * by the query parameters.
    *
    * The depth parameters specify how far forward or backward to traverse the lineage
    * graph to collect lineage.
    *
    *   Query parameter ?bw=0 produces full forward lineage
    *   Query parameter ?fw=0 produces full backward lineage
    *   Query parameter ?bw=X (where X > 0) produces backward depth X and forward depth 0
    *   Query parameter ?fw=X (where X > 0) produces forward depth X and backward depth 0
    *   If both query parameters are given ?bw=X&fw=Y (for any X and Y), use those depths
    *   If neither query parameter is given, use full depth in each direction
    *
    * @param name The name of the table for which to search table lineage.
    * @param backwardParam The number of levels upstream for which to collect lineage.
    * @param forwardParam The number of levels downstream for which to collect lineage.
    * @return A JSON payload with the queried lineage subgraph.
    */
  def table(name: String, backwardParam: Option[String], forwardParam: Option[String]): Action[AnyContent] =
    LineageAction.async { implicit request =>

      /**
        * Given the parsed query parameters, use the LineageService to fetch the results.
        */
      def lookupTableLineage(backwardDepth: Option[Int], forwardDepth: Option[Int]): Future[Graph] = {
        // If only one depth parameter is given, set the other one to zero.
        val (bwDepth, fwDepth) = (backwardDepth, forwardDepth) match {
          // Query parameter ?bw=0&fw=0 produces full forward lineage
          case (Some(0), Some(0)) => (Some(0), None)
          // Query parameter ?bw=0 produces full forward lineage
          case (Some(0), None) => (Some(0), None)
          // Query parameter ?fw=0 produces full backward lineage
          case (None, Some(0)) => (None, Some(0))
          // Query parameter ?bw=X (where X > 0) produces backward depth X and forward depth 0
          case (Some(bw), None) => (Some(bw), Some(0))
          // Query parameter ?fw=X (where X > 0) produces forward depth X and backward depth 0
          case (None, Some(fw)) => (Some(0), Some(fw))
          // If both query parameters are given ?bw=X&fw=Y (for any X and Y), use those depths
          // Or, if neither query parameter is given, use full depth in each direction
          case depths => depths
        }

        // Use the LineageService to calculate lineage with the given parameters
        cc.lineageService.tableLineage(name, bwDepth, fwDepth)
      }

      /**
        * Parses the query values from the query parameters, then calls
        * [[lookupTableLineage]] to produce the lineage result.
        *
        * @param backwardParam The raw query string for backward depth.
        * @param forwardParam The raw query string for forward depth.
        * @return A Future with the HTTP response to return to the client.
        */
      def handleRequest(backwardParam: Option[String], forwardParam: Option[String]): Future[Result] = {

        // Try to parse the query parameters as Ints
        val maybeBackwardInt = backwardParam.map(bw => Try(bw.trim.toInt))
        val maybeForwardInt = forwardParam.map(fw => Try(fw.trim.toInt))

        // Unwrap the values of the query parameters parsed as Ints
        // Or, return a 400 response if either parameter does not parse as an Int
        val (backward, forward) = (maybeBackwardInt, maybeForwardInt) match {
          // Failure cases. If either parameter failed to parse as an Int, return 400
          case (Some(Failure(_)), Some(Failure(_))) => return Future.successful(BadRequest("Query parameters 'bw' and 'fw' for lineage depth must be integers"))
          case (Some(Failure(_)), _)                => return Future.successful(BadRequest("Query parameter 'bw' for backward depth must be an integer"))
          case (_, Some(Failure(_)))                => return Future.successful(BadRequest("Query parameter 'fw' for forward depth must be an integer"))
          // Success cases. Unwrap the parameters if they're present.
          case (Some(Success(bw)), Some(Success(fw))) => (Some(bw), Some(fw))
          case (None, Some(Success(fw)))              => (None, Some(fw))
          case (Some(Success(bw)), None)              => (Some(bw), None)
          case (None, None)                           => (None, None)
        }

        // Pass the parsed query values to the lookup helper to get lineage
        val lineage = lookupTableLineage(backward, forward)

        // Produce a proper HTTP response for different success cases from LineageService
        lineage.transform {
          // A failure fetching lineage produces a 500 error code
          case Failure(_) => Success(InternalServerError(s"""Unexpected error constructing lineage for table "$name""""))
          // A successful query that is empty produces a 404
          case Success(Graph(nodes, links)) if nodes.isEmpty && links.isEmpty => Success(NotFound(s"""Table "$name" not found"""))
          // Any other successful query produces a 200 with the JSON graph
          case Success(graph) => Success(Ok(Json.toJson(graph)))
        }
      }

      handleRequest(backwardParam, forwardParam)
    }
}
