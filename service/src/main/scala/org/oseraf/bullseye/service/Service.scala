package org.oseraf.bullseye.service

import com.typesafe.config.ConfigFactory
import java.io.File

trait Service {
  private val baseConfig = {
    val bconf = ConfigFactory.load().getConfig("bullseye")
    if (bconf.hasPath("overrides")) {
      val overrides = ConfigFactory.parseFile(new File(bconf.getString("overrides")))
      overrides.withFallback(bconf)
    } else {
      bconf
    }
  }

  val conf = baseConfig.getConfig("service")
}
