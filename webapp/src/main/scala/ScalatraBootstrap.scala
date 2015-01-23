import javax.servlet.ServletContext

import org.oseraf.bullseye.webapp.api.DataAPI
import org.scalatra.scalate.ScalateSupport
import org.scalatra.{LifeCycle, ScalatraServlet}


class DefaultServlet extends ScalatraServlet with ScalateSupport {
  get("/") {
    contentType = "text/html; charset=UTF-8"
    response.setHeader("X-UI-Compatible", "IE-edge")
    ssp("index")
  }
}

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new DefaultServlet, "/", "bullseye")
    context.mount(new DataAPI {}, "/rest/data", "bullseye/data")
  }

}
