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

//object GraphLoader extends GraphLoader {
//  def searchTypes(g:Graph): Seq[BullsEyeSearchType] =
//    Seq(BullsEyeSearchType("_id", "ID")) ++
//      g.getVertices
//        .foldLeft(Set[String]()) { case (attrs, vertex) => attrs ++ vertex.getPropertyKeys }
//        .map(key => BullsEyeSearchType(key, cleanAttrName(key)))
//        .toSeq
//
//  private def cleanAttrName(str: String): String = {
//    // camel-case splitter from
//    //    http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
//    str.replaceAll(
//      String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"),
//      " "
//    ).split(Array(' ', '_')).map(_.capitalize).mkString(" ")
//  }
//}
