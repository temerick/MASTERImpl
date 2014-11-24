package org.oseraf.bullseye.webapp.api

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

trait APIMarshalling extends JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
}

trait API extends ScalatraServlet with APIMarshalling {
  before() {
    contentType = formats("json")
  }
}
