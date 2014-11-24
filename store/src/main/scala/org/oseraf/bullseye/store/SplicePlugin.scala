package org.oseraf.bullseye.store

/**
 * Created by nhamblet.
 */
trait SplicePlugin {
  type ArgumentEntityStore = SplicePlugin.SpliceableStore

  val store: EntityStore

  def splice(fromPortion: ArgumentEntityStore, toPortion: ArgumentEntityStore): Boolean =
    spliceEntities(fromPortion, toPortion) && spliceRelationships(fromPortion, toPortion)

  def spliceEntities(fromPortion: ArgumentEntityStore, toPortion: ArgumentEntityStore): Boolean = {
    val fromIds = fromPortion.entities.toSet
    val updates = fromIds.foldLeft(true) {
      case (successSoFar, curId) => successSoFar && updateOrRemoveEntity(toPortion, curId)
    }
    val toIds = toPortion.entities.toSet
    val inserts = toIds.foldLeft(true) {
      case (successSoFar, curId) => successSoFar && insertEntityIfNecessary(toPortion, curId, fromIds)
    }
    updates && inserts
  }

  def updateOrRemoveEntity(toPortion: ArgumentEntityStore, id: EntityStore.ID): Boolean = {
    if (toPortion.entityExists(id)) {
      updateEntity(toPortion, id)
    } else {
      store.removeEntity(id)
    }
    true
  }

  def updateEntity(toPortion: ArgumentEntityStore, id: EntityStore.ID): Boolean = {
    val entityInToPortion = toPortion.entity(id)
    val attributeBehavior = entityInToPortion.attribute(SplicePlugin.ATTRIBUTE_BEHAVIOR_KEY, SplicePlugin.DEFAULT_ATTRIBUTE_BEHAVIOR)
    if (attributeBehavior == SplicePlugin.ATTRIBUTE_BEHAVIOR__REPLACE) {
      store.updateEntity(id, entityInToPortion)
    }
    true
  }

  def insertEntityIfNecessary(toPortion: ArgumentEntityStore, id: EntityStore.ID, unnecessaryIds: Set[EntityStore.ID]): Boolean = {
    if (!unnecessaryIds.contains(id) && !store.entityExists(id)) {
      store.addEntity(id, toPortion.entity(id))
    }
    true
  }
  
  def spliceRelationships(fromPortion: ArgumentEntityStore, toPortion: ArgumentEntityStore): Boolean = {
    val fromIds = fromPortion.relationships.toSet
    val updates = fromIds.foldLeft(true) {
      case (successSoFar, curId) => successSoFar && updateOrRemoveRelationship(toPortion, curId)
    }
    val toIds = toPortion.relationships.toSet
    val inserts = toIds.foldLeft(true) {
      case (successSoFar, curId) => successSoFar && insertRelationshipIfNecessary(toPortion, curId, fromIds)
    }
    updates && inserts
  }

  def updateOrRemoveRelationship(toPortion: ArgumentEntityStore, id: EntityStore.ID): Boolean = {
    if (toPortion.relationshipExists(id)) {
      updateRelationship(toPortion, id)
    } else {
      store.removeRelationship(id)
    }
    true
  }

  def updateRelationship(toPortion: ArgumentEntityStore, id: EntityStore.ID): Boolean = {
    val relationshipInToPortion = toPortion.relationship(id)
    val attributeBehavior = relationshipInToPortion.attribute(SplicePlugin.ATTRIBUTE_BEHAVIOR_KEY, SplicePlugin.DEFAULT_ATTRIBUTE_BEHAVIOR)
    if (attributeBehavior == SplicePlugin.ATTRIBUTE_BEHAVIOR__REPLACE) {
      store.updateRelationship(id, relationshipInToPortion)
    }
    true
  }

  def insertRelationshipIfNecessary(toPortion: ArgumentEntityStore, id: EntityStore.ID, unnecessaryIds: Set[EntityStore.ID]): Boolean = {
    if (!unnecessaryIds.contains(id) && !store.relationshipExists(id)) {
      store.addRelationship(id, toPortion.relationship(id))
    }
    true
  }
}


object SplicePlugin {
  final val ATTRIBUTE_BEHAVIOR_KEY = "OSERAF:splice/attr/behavior"
  final val ATTRIBUTE_BEHAVIOR__IGNORE = "ignore"
  final val ATTRIBUTE_BEHAVIOR__REPLACE = "replace"
  final val DEFAULT_ATTRIBUTE_BEHAVIOR = ATTRIBUTE_BEHAVIOR__IGNORE

  final type SpliceableStore = EntityStore with EntityIterationPlugin with RelationshipIterationPlugin
}