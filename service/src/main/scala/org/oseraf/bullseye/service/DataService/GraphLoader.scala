package org.oseraf.bullseye.service.DataService

import com.thinkaurelius.titan.core.TitanFactory
import com.tinkerpop.blueprints._
import com.typesafe.config.Config
import org.apache.commons.configuration.BaseConfiguration
import org.oseraf.bullseye.service.Service

import scala.collection.JavaConversions._

object GraphLoader extends Service {
  def createGraph(conf: Config): Graph = {
    val gProps = conf.getList("factoryArgs").toList.map(_.render)
    val aConf = new BaseConfiguration
    gProps.foreach(setting => {
      val s = setting.split("=")
      val key = s(0).tail                   //remove the first quote
      val value = s(1).dropRight(1)         //remove the last quote
      aConf.setProperty(key, value)
    })
    if(conf.getBoolean("titan")) {     //kinda sad that we couldn't get this to work with the Blueprints GraphFactory :(
      TitanFactory.open(aConf)
    }
    else {
      GraphFactory.open(aConf)
    }
  }
}
