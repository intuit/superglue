package v1.lineage

import com.intuit.superglue.dao.SuperglueRepository
import com.intuit.superglue.lineage.LineageService
import javax.inject.Inject
import net.logstash.logback.marker.LogstashMarker
import play.api.{Logger, MarkerContext}
import play.api.http.{FileMimeTypes, HttpVerbs}
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait LineageRequestHeader
  extends MessagesRequestHeader
    with PreferredMessagesProvider

class LineageRequest[A](request: Request[A], val messagesApi: MessagesApi)
  extends WrappedRequest(request)
    with LineageRequestHeader

trait RequestMarkerContext {
  import net.logstash.logback.marker.Markers

  private def marker(tuple: (String, Any)) = Markers.append(tuple._1, tuple._2)

  private implicit class RichLogstashMarker(marker1: LogstashMarker) {
    def &&(marker2: LogstashMarker): LogstashMarker = marker1.and(marker2)
  }

  implicit def requestHeaderToMarkerContext(implicit request: RequestHeader): MarkerContext = {
    MarkerContext {
      marker("id" -> request.id) && marker("host" -> request.host) && marker("remoteAddress" -> request.remoteAddress)
    }
  }
}

class LineageActionBuilder @Inject()(messagesApi: MessagesApi,
  playBodyParsers: PlayBodyParsers)(
  implicit val executionContext: ExecutionContext)
  extends ActionBuilder[LineageRequest, AnyContent]
    with RequestMarkerContext
    with HttpVerbs {

  override val parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  private val logger = Logger(this.getClass)

  type LineageRequestBlock[A] = LineageRequest[A] => Future[Result]

  override def invokeBlock[A](
    request: Request[A],
    block: LineageRequestBlock[A],
  ): Future[Result] = {
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(request)
    logger.trace(s"invokeBlock: ")

    val future = block(new LineageRequest(request, messagesApi))
    future.map { result =>
      request.method match {
        case GET | HEAD => result.withHeaders("Cache-Control" -> "max-age: 100")
        case _ => result
      }
    }
  }
}

case class LineageControllerComponents @Inject()(
  lineageActionBuilder: LineageActionBuilder,
  superglueRepository: SuperglueRepository,
  lineageService: LineageService,
  actionBuilder: DefaultActionBuilder,
  parsers: PlayBodyParsers,
  messagesApi: MessagesApi,
  langs: Langs,
  fileMimeTypes: FileMimeTypes,
  executionContext: ExecutionContext,
) extends ControllerComponents

class LineageBaseController @Inject()(cc: LineageControllerComponents) extends BaseController with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = cc

  def LineageAction: LineageActionBuilder = cc.lineageActionBuilder
}
