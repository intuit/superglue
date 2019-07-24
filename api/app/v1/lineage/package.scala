package v1

import com.intuit.superglue.lineage.model.Node.TableNode
import com.intuit.superglue.lineage.model.{Graph, Link, Node}
import play.api.libs.json._

package object lineage {

  implicit val nodeWrites: Writes[Node] = {
    case tn: TableNode => JsObject(Map(
      "id" -> JsNumber(tn.id),
      "group" -> JsString(tn.group),
      "label" -> JsString(tn.name),
    ))
  }

  implicit val edgeWrites: Writes[Link] = (link: Link) => JsObject(Map(
    "from" -> Json.toJson(link.sourceNode.id),
    "to" -> Json.toJson(link.destinationNode.id),
  ))

  implicit val graphWrites: Writes[Graph] = (graph: Graph) => JsObject(Map(
    "nodes" -> Json.toJson(graph.nodes),
    "edges" -> Json.toJson(graph.links),
  ))
}
