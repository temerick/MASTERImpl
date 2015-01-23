package org.oseraf.bullseye.service

import com.typesafe.config.ConfigFactory
import java.io.File
import com.typesafe.scalalogging.slf4j.Logging

trait Service extends Logging {
  private val baseConfig = {
    val bconf = ConfigFactory.load().getConfig("bullseye")
    if (bconf.hasPath("overrides")) {
      logger.info("hasPath: overrides")
      /*val overrides = */ConfigFactory.parseFile(new File(bconf.getString("overrides")))
      //overrides.withFallback(bconf)
    } else {
      bconf
    }
  }
  val conf = baseConfig.getConfig("service")
  logger.info("Loaded clazz: " + conf.getConfig("store").getString("clazz"))
}
