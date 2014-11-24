package org.oseraf.bullseye.service

import com.typesafe.config.ConfigFactory

trait Service {
  val conf = ConfigFactory.load().getConfig("bullseye").getConfig("service")
}
