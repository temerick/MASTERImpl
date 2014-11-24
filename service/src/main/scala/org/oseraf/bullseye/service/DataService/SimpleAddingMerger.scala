package org.oseraf.bullseye.service.DataService

import java.util.UUID

import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store.AttributeContainer.{KEY, VALUE}
import org.oseraf.bullseye.store.EntityStore.ID
import org.oseraf.bullseye.store._
import org.oseraf.bullseye.store.impl.blueprints.BlueprintsGraphStore

trait SimpleAddingMerger extends Service {

  val store: EntityStore with SplicePlugin

  def merge(eIds:Seq[ID], mergeNeighborhood: SplicePlugin.SpliceableStore, targetId: ID): Entity = {
    val fromStore = BlueprintsGraphStore(new TinkerGraph())
    val toStore = BlueprintsGraphStore(new TinkerGraph())

    val mergedEntity = IdentifiedEntity(targetId, mergeNeighborhood.entity(targetId))

    // effectively copy mergeNeighborhood to toStore
    for (entityId <- mergeNeighborhood.entities) {
      toStore.addEntity(entityId, mergeNeighborhood.entity(entityId))
    }
    for (relationshipId <- mergeNeighborhood.relationships) {
      toStore.addRelationship(relationshipId, mergeNeighborhood.relationship(relationshipId))
    }

    // add old nodes to both stores, with appropriate attribute flags, and edges
    for (eId <- eIds) {
      fromStore.addEntity(eId, Entity())
      val toEntity = new Entity {
        override var attributes = Map(SplicePlugin.ATTRIBUTE_BEHAVIOR_KEY -> SplicePlugin.ATTRIBUTE_BEHAVIOR__IGNORE)
      }
      if (!toStore.entityExists(eId)) {
        toStore.addEntity(eId, toEntity)
      }
      toStore.addRelationship(UUID.randomUUID().toString, equivalenceRelationship(eId, mergedEntity.id))
    }
    store.splice(fromStore, toStore)
    store.entity(mergedEntity.id)
  }

  def equivalenceRelationship(fromId: ID, toId: ID): Relationship = {
    new BinaryRelationship {
      override def isDirected: Boolean = false
      override def from: ID = fromId
      override def to: ID = toId
      override var attributes: Map[KEY, VALUE] =
        Map(SimpleAddingMerger.EQUIVALENCE_RELATION_KEY -> SimpleAddingMerger.EQUIVALENCE_RELATION_VAL)
    }
  }
}

object SimpleAddingMerger {
  final val EQUIVALENCE_RELATION_KEY = "OSERAF:edgeType"
  final val EQUIVALENCE_RELATION_VAL = "isEquivalentTo"
}


trait SimpleAddingMergerIdentifier {
  def targetId(eIds:Seq[ID], targetId: ID) = targetId
}