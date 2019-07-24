package v1.lineage

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class LineageRouter @Inject()(controller: LineageController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/table/$name" ? q_?"bw=$bw" & q_?"fw=$fw") => controller.table(name, bw, fw)
  }
}
