package org.oseraf.bullseye.ikanow

import java.util.UUID

import com.thinkaurelius.titan.core.{TitanGraph, TitanTransaction}
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.Logging
import org.apache.commons.codec.digest.DigestUtils
import org.oseraf.bullseye.service.DataService._
import org.oseraf.bullseye.store.AttributeContainer.{KEY, VALUE}
import org.oseraf.bullseye.store._
import org.oseraf.bullseye.store.impl.blueprints.{BlueprintsGraphStore, IndexedBlueprintsGraphStore}

class IkanowFallBackStore
  extends BullseyeEntityStore
    with AttributeBasedNaivelyFuzzySearchPlugin
    with AttributeBasedSearchAvailableOptionsPlugin
    with EntityIterationPlugin
    with RelationshipIterationPlugin
    with NeighborhoodPlugin
    with SplicePlugin
    with Logging
{
  var blueprintsGraphStore: BlueprintsGraphStore with WriteEventPublisherPlugin = null
  var ikanowRetriever: IkanowRetriever = null

  def setup(conf: Config) {
    val ikanowConf = conf.getConfig("ikanow")
    ikanowRetriever =
      new IkanowRetriever(
        ikanowConf.getString("url"),
        ikanowConf.getString("user"),
        ikanowConf.getString("password"),
        ikanowConf.getString("communities").split(",")
      )
    val graph = GraphLoader.createGraph(conf.getConfig("graph"))
    blueprintsGraphStore = new IndexedBlueprintsGraphStore(graph) with WriteEventPublisherPlugin
  }

  val mergeIdentifier = new SimpleAddingMergerIdentifier {
    override def targetId(eIds:Seq[EntityStore.ID], targetId: EntityStore.ID) =
      IkanowFallBackStore.randomMasterEntityId()
  }
  val splitIdentifier = new SimpleAddingSplitterIdentifier {
    override def targetId(splitFromId: EntityStore.ID, targetSplitId: EntityStore.ID): EntityStore.ID =
      IkanowFallBackStore.randomMasterEntityId()
  }

  override def search(key: AttributeContainer.KEY, value: AttributeContainer.VALUE): Iterable[(EntityStore.ID, Double)] = {
    if (fallbackSearchAttributes.contains(key)) {
      logger.debug("Querying ikanow for entity suggestions for " + value)
      val ikanowEntities = ikanowRetriever.getEntities(value)
      var transaction: TitanTransaction = null
      if (blueprintsGraphStore.graph.isInstanceOf[TitanGraph]) {
        transaction = blueprintsGraphStore.graph.asInstanceOf[TitanGraph].newTransaction()
      }
      logger.trace("Found " + ikanowEntities.size)
      for (ent <- ikanowEntities) {
        addEntIfNecessary(ent)
      }
      if (transaction != null) {
        transaction.commit()
      }
    }
    logger.debug(s"Querying blueprints for $key = $value")
    blueprintsGraphStore.search(key, value)
  }

  lazy val searchableAttributes =
    List(
      (AttributeBasedSearch.FAKE_ID_ATTRIBUTE_KEY, "ID"),
      (IkanowFallBackStore.IKANOW_VALUE_ATTR_KEY, IkanowFallBackStore.IKANOW_VALUE_ATTR_NAME),
      (IkanowFallBackStore.IKANOW_TYPE_ATTR_KEY, IkanowFallBackStore.IKANOW_TYPE_ATTR_NAME),
      (IkanowFallBackStore.IKANOW_DIMENSION_ATTR_KEY, IkanowFallBackStore.IKANOW_DIMENSION_ATTR_NAME)
    )

  lazy val fallbackSearchAttributes =
    Set(
      AttributeBasedSearch.FAKE_ID_ATTRIBUTE_KEY,
      IkanowFallBackStore.IKANOW_VALUE_ATTR_KEY
    )

  def addEntIfNecessary(ent: IkanowEntity) = {
    logger.trace(s"Looking to add ${ent.eId}")
    if (!blueprintsGraphStore.entityExists(ent.eId)) {
      logger.trace(s"""Needs ${ent.eId}, adding it (${ent.attrs("Name")})""")
      blueprintsGraphStore.addEntity(ent.eId, new Entity {
        override var attributes = ent.attrs
      })
    }
  }

  override def neighborhood(entityId: EntityStore.ID) = {
    if (IkanowFallBackStore.isIkanowEntity(entityId)) {
      logger.debug(s"Calculating neighborhood of Ikanow entity $entityId")
      val node = entity(entityId)
      val ikValue = node.attribute(IkanowFallBackStore.IKANOW_VALUE_ATTR_KEY)
      val ikType = node.attribute(IkanowFallBackStore.IKANOW_TYPE_ATTR_KEY)
      logger.trace(s"Looking up neighborhood of $entityId as $ikValue  --  $ikType")
      val docs = ikanowRetriever.getNeighborhoodDocuments(ikValue, ikType)
      logger.trace(s"Found ${docs.size} docs")
      docs.foreach(doc => {
        val docEntId = IkanowFallBackStore.ikanowDocumentId(doc._id)
        if (!blueprintsGraphStore.entityExists(docEntId)) {
          blueprintsGraphStore.addEntity(docEntId, new Entity {
            override var attributes = Map("Title" -> doc.title, "DocId" -> doc._id, "Name" -> doc.title)
          })
          doc.entities.foreach(jsonEntity => {
            val ikEnt = ikanowRetriever.makeIkanowEntity(jsonEntity)
            addEntIfNecessary(ikEnt)
            val relId = UUID.randomUUID().toString
            logger.trace(s"Adding edge $relId connecting $docEntId to ${ikEnt.eId}")
            blueprintsGraphStore.addRelationship(
              relId,
              new Relationship {
                override def connecting(): Iterable[EntityStore.ID] = {
                  Seq(docEntId, ikEnt.eId)
                }
                override var attributes: Map[KEY, VALUE] =
                  Map("OSERAF:ikanow/edgeType" -> "mentions")
              }
            )
          })
        }
      })
    }
    logger.trace(s"Passing neighborhood call for $entityId off to blueprints")
    blueprintsGraphStore.neighborhood(entityId)
  }

  override lazy val store = blueprintsGraphStore.store

  override def entityExists(id: EntityStore.ID) = blueprintsGraphStore.entityExists(id)
  override def relationshipExists(id: EntityStore.ID) = blueprintsGraphStore.relationshipExists(id)
  override def entity(id: EntityStore.ID) = blueprintsGraphStore.entity(id)
  override def relationship(id: EntityStore.ID) = blueprintsGraphStore.relationship(id)
  override def addEntity(id: EntityStore.ID, entity: Entity) = blueprintsGraphStore.addEntity(id, entity)
  override def addRelationship(id: EntityStore.ID, relationship: Relationship) = blueprintsGraphStore.addRelationship(id, relationship)
  override def removeEntity(id: EntityStore.ID) = blueprintsGraphStore.removeEntity(id)
  override def removeRelationship(id: EntityStore.ID) = blueprintsGraphStore.removeRelationship(id)
  override def updateEntity(id: EntityStore.ID, entity: Entity) = blueprintsGraphStore.updateEntity(id, entity)
  override def updateRelationship(id: EntityStore.ID, relationship: Relationship) = blueprintsGraphStore.updateRelationship(id, relationship)

  override def entities = blueprintsGraphStore.entities
  override def relationships = blueprintsGraphStore.relationships

  override def splice(fromPortion: ArgumentEntityStore, toPortion: ArgumentEntityStore): Boolean = {
    // first, make sure all new entities have the MASTER_SOURCE attribute
    toPortion.entities.foreach(entId => {
      if (!blueprintsGraphStore.entityExists(entId)) {
        toPortion.updateEntity(entId, new Entity {
          override var attributes =
            toPortion.entity(entId).attributes
              .updated(IkanowFallBackStore.MASTER_ENTITY_SOURCE_ATTR_KEY, IkanowFallBackStore.MASTER_KEY)
        })
      }
    })
    super.splice(fromPortion, toPortion)
  }

  override def resolutionStore = blueprintsGraphStore
  override def spliceStore = this
}

object IkanowFallBackStore {

  final val IKANOW_VALUE_ATTR_KEY = "Name"
  final val IKANOW_VALUE_ATTR_NAME = "Name"

  final val IKANOW_TYPE_ATTR_KEY = "Type"
  final val IKANOW_TYPE_ATTR_NAME = "Type"

  final val IKANOW_DIMENSION_ATTR_KEY = "Dimension"
  final val IKANOW_DIMENSION_ATTR_NAME = "Dimension"
  
  final val MASTER_ENTITY_SOURCE_ATTR_KEY = "MASTER:source" // either of the following
  final val IKANOW_KEY = "ikanow"
  final val MASTER_KEY = "master"
  
  final val MASTER_ENTITY_IKANOW_TYPE_ATTR_KEY = "MASTER:ikanow/type" // either of the following
  final val IKANOW_ENTITY_TYPE = "entity"
  final val IKANOW_DOCUMENT_TYPE = "document"

  def ikanowEntityId(ikValue: String, ikType: String): EntityStore.ID =
    IKANOW_KEY + ":" + IKANOW_ENTITY_TYPE + ":" + safeIkId(ikValue, ikType)

  private def safeIkId(ikValue: String, ikType: String): String =
    whiten(ikValue + "/" + ikType)

  private def whiten(str: String): String =
    DigestUtils.sha1Hex(str)

  def ikanowDocumentId(ikId: String): EntityStore.ID =
    IKANOW_KEY + ":" + IKANOW_DOCUMENT_TYPE + ":" + whiten(ikId)

  def randomMasterEntityId(): EntityStore.ID =
    MASTER_KEY + ":" + UUID.randomUUID().toString

  def isIkanowEntity(entityId: EntityStore.ID): Boolean =
    entityId.startsWith(IKANOW_KEY + ":" + IKANOW_ENTITY_TYPE + ":")
}