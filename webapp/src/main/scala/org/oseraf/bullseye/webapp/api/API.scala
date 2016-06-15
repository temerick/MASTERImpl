package org.oseraf.bullseye.webapp.api

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{ScalatraServlet,CorsSupport}
import org.scalatra.json.JacksonJsonSupport

trait APIMarshalling extends JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
}

trait API extends ScalatraServlet with APIMarshalling with CorsSupport {
  before() {
    contentType = formats("json")
    options("/*"){
      response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
    }
  }
}
