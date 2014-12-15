package org.oseraf.bullseye.ikanow

import com.ikanow.infinit.e.data_model.api.ResponsePojo._
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryTermPojo
import com.ikanow.infinit.e.data_model.api.knowledge._
import com.typesafe.scalalogging.slf4j.Logging
import org.bson.types.ObjectId
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConverters._

case class IkanowEntity(eId:String, attrs:Map[String,String])
case class IkanowRelationship(source:IkanowEntity, target:IkanowEntity, attrs:Map[String,String])
case class IkanowDocument(_id: String, title:String, entities:Seq[Map[String,_]], mediaTypes:Seq[String])
class IkanowRetriever(urlString:String, userName:String, password:String, communities:Iterable[String])
  extends Logging
{   //this should be an object, but I want parameters -- FIX

  def makeIkanowEntity(entityJson: Map[String, _]): IkanowEntity = {
    IkanowEntity(
      IkanowFallBackStore.ikanowEntityId(entityJson("disambiguated_name").toString, entityJson("type").toString),
      Map(
        IkanowFallBackStore.IKANOW_VALUE_ATTR_KEY -> entityJson("disambiguated_name").toString,
        IkanowFallBackStore.IKANOW_TYPE_ATTR_KEY -> entityJson("type").toString,
        IkanowFallBackStore.IKANOW_DIMENSION_ATTR_KEY -> entityJson("dimension").toString,
        IkanowFallBackStore.MASTER_ENTITY_SOURCE_ATTR_KEY -> IkanowFallBackStore.IKANOW_KEY,
        IkanowFallBackStore.MASTER_ENTITY_IKANOW_TYPE_ATTR_KEY -> IkanowFallBackStore.IKANOW_ENTITY_TYPE
      )
    )
  }

  def makeIkanowEntity(entity:SearchSuggestPojo): IkanowEntity =
    IkanowEntity(
      IkanowFallBackStore.ikanowEntityId(entity.getValue, entity.getType),
      Map(
        IkanowFallBackStore.IKANOW_VALUE_ATTR_KEY -> entity.getValue,
        IkanowFallBackStore.IKANOW_TYPE_ATTR_KEY -> entity.getType,
        IkanowFallBackStore.IKANOW_DIMENSION_ATTR_KEY -> entity.getDimension,
        IkanowFallBackStore.MASTER_ENTITY_SOURCE_ATTR_KEY -> IkanowFallBackStore.IKANOW_KEY,
        IkanowFallBackStore.MASTER_ENTITY_IKANOW_TYPE_ATTR_KEY -> IkanowFallBackStore.IKANOW_ENTITY_TYPE
      )
    )

  lazy val infDriver = {
    val infDriver = new InfiniteDriver(urlString, userName, password)
    infDriver.login()
    infDriver
  }

  lazy val communityIds =
    communities.map(x => new ObjectId(x)).toList

  def getAllEntitySuggestions(term:String):Seq[SearchSuggestPojo] = {
    refreshLogin()
    val responseObject = new ResponseObject
    infDriver.getEntitySuggest(term, communityIds.mkString(","), false, false, responseObject).dimensions.asScala.toSeq
  }

  def getEntities(term:String):Seq[IkanowEntity] = {
    refreshLogin()
    val responseObject = new ResponseObject()
    val qry = new AdvancedQueryPojo
    val term1 = new QueryTermPojo
    term1.ftext = term

    qry.qt = List(term1).asJava
    val documents = infDriver.sendQuery(qry, communityIds.asJava, responseObject)
    val scalaSONDocs = parse(documents.getData.toString).values.asInstanceOf[Seq[Map[String, _]]]
    val ents = scalaSONDocs.flatMap(_("entities").asInstanceOf[Seq[Map[String,_]]])
    ents.map(makeIkanowEntity)
  }

  def getNeighborhoodDocuments(entityValue: String, entityType: String):Seq[IkanowDocument] = {
    refreshLogin()
    val responseObject = new ResponseObject()
    val qry = new AdvancedQueryPojo
    val term1 = new QueryTermPojo

    term1.entityType = entityType
    term1.entityValue = entityValue
    qry.qt = List(term1).asJava
//    qry.logic = "1"
    val documents = infDriver.sendQuery(qry, communityIds.asJava, responseObject)

    //parse documents.getData using json4s
    val scalaSONDocs = parse(documents.getData.toString).values.asInstanceOf[Seq[Map[String, _]]]
    scalaSONDocs.map(doc =>
      IkanowDocument(
        doc("_id").toString,
        doc("title").toString,
        doc("entities").asInstanceOf[Seq[Map[String,_]]],
        doc("mediaType").asInstanceOf[Seq[String]]
      )
    )
  }

  def refreshLogin() = {
    if (!infDriver.sendKeepalive()) {
      logger.info("Keep-alive failed, logging back in")
      infDriver.login()
    }
  }

  def getAllEntityTypes(suggestions:Seq[SearchSuggestPojo]):Set[String] =
    suggestions.map(_.getType).filter(_ != null).toSet

  def getAllWs(suggestions:Seq[SearchSuggestPojo]):Set[String] =
    suggestions.map(_.getDimension).filter(_ != null).toSet

  def getAllOntologyTypes(suggestions:Seq[SearchSuggestPojo]):Set[String] =
    suggestions.map(_.getOntology_type).filter(_ != null).toSet
}
