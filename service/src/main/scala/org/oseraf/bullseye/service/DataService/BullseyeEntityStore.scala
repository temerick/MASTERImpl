package org.oseraf.bullseye.service.DataService

import com.tinkerpop.blueprints.Graph
import com.typesafe.config.Config
import org.oseraf.bullseye.store._
import org.oseraf.bullseye.store.impl.blueprints.{IndexedBlueprintsGraphStore, BlueprintsGraphStore}

/**
 * Created by nhamblet.
 */
trait BullseyeEntityStore
  extends EntityStore
     with SplicePlugin
     with NeighborhoodPlugin
     with AttributeBasedSearchAvailableOptionsPlugin
     with AttributeBasedNaivelyFuzzySearchPlugin
     with EntityIterationPlugin
{
  def setup(conf: Config)
  def spliceStore: EntityStore with SplicePlugin
  def resolutionStore: EntityStore with EntityIterationPlugin with WriteEventPublisherPlugin
  def mergeIdentifier: SimpleAddingMergerIdentifier
  def splitIdentifier: SimpleAddingSplitterIdentifier
}


class BlueprintsBullseyeEntityStore
  extends BullseyeEntityStore {
  
  var graph: Graph = null
  var innerStore: BlueprintsGraphStore with WriteEventPublisherPlugin = null
  
  override def setup(conf: Config) = {
    graph = GraphLoader.createGraph(conf)
    innerStore = new BlueprintsGraphStore(graph) with WriteEventPublisherPlugin
  }
  
  override def spliceStore = innerStore
  override def resolutionStore = innerStore
  override def mergeIdentifier = new SimpleAddingMergerIdentifier {}
  override def splitIdentifier = new SimpleAddingSplitterIdentifier {}

  def search(key: AttributeContainer.KEY, value: AttributeContainer.VALUE) = innerStore.search(key, value)
  def searchableAttributes = innerStore.searchableAttributes
  
  def addEntity(id: EntityStore.ID, entity: Entity) = innerStore.addEntity(id, entity)
  def addRelationship(id: EntityStore.ID, relationship: Relationship) = innerStore.addRelationship(id, relationship)
  def entity(id: EntityStore.ID) = innerStore.entity(id)
  def entityExists(id: EntityStore.ID) = innerStore.entityExists(id)
  def relationship(id: EntityStore.ID) = innerStore.relationship(id)
  def relationshipExists(id: EntityStore.ID) = innerStore.relationshipExists(id)
  def removeEntity(id: EntityStore.ID) = innerStore.removeEntity(id)
  def removeRelationship(id: EntityStore.ID) = innerStore.removeRelationship(id)
  def updateEntity(id: EntityStore.ID, entity: Entity) = innerStore.updateEntity(id, entity)
  def updateRelationship(id: EntityStore.ID, relationship: Relationship) = innerStore.updateRelationship(id, relationship)
  def neighborhood(entityId: EntityStore.ID) = innerStore.neighborhood(entityId)
  def entities = innerStore.entities
  
  val store: EntityStore = innerStore
}

class IndexedBlueprintsBullseyeEntityStore extends BlueprintsBullseyeEntityStore {
  override def setup(conf: Config) = {
    graph = GraphLoader.createGraph(conf)
    innerStore = new IndexedBlueprintsGraphStore(graph) with WriteEventPublisherPlugin
  }
}