package org.oseraf.bullseye.store

import scala.collection.mutable

/**
 * Created by nhamblet.
 */
trait WriteEventPublisherPlugin extends EntityStore {
  var writeListeners = new mutable.ArrayBuffer[WriteEventListener]()

  abstract override def addEntity(id: EntityStore.ID, entity: Entity): Boolean =
    returning(super.addEntity(id, entity)) { writeListeners.foreach(_.handleAddEntityEvent(id, entity)) }
  abstract override def addRelationship(id: EntityStore.ID, relationship: Relationship): Boolean =
    returning(super.addRelationship(id, relationship)) { writeListeners.foreach(_.handleAddRelationshipEvent(id)) }

  abstract override def updateEntity(id: EntityStore.ID, entity: Entity): Boolean =
    returning(super.updateEntity(id, entity)) { writeListeners.foreach(_.handleUpdateEntityEvent(id)) }
  abstract override def updateRelationship(id: EntityStore.ID, relationship: Relationship): Boolean =
    returning(super.updateRelationship(id, relationship)) { writeListeners.foreach(_.handleUpdateRelationshipEvent(id)) }

  abstract override def removeEntity(id: EntityStore.ID): Boolean =
    returning(super.removeEntity(id)) { writeListeners.foreach(_.handleRemoveEntityEvent(id)) }
  abstract override def removeRelationship(id: EntityStore.ID): Boolean =
    returning(super.removeRelationship(id)) { writeListeners.foreach(_.handleRemoveRelationshipEvent(id)) }

  def addListener(listener: WriteEventListener) =
    writeListeners.append(listener)

  def removeListener(listener: WriteEventListener) =
    writeListeners.filter(_ != listener)

  private def returning(b: Boolean)(blk: => Unit) = {
    val res = b
    if (res) blk
    res
  }
}

trait WriteEventListener {
  def handleAddEntityEvent(id: EntityStore.ID, entity: Entity)
  def handleAddRelationshipEvent(id: EntityStore.ID)

  def handleRemoveEntityEvent(id: EntityStore.ID)
  def handleRemoveRelationshipEvent(id: EntityStore.ID)

  def handleUpdateEntityEvent(id: EntityStore.ID)
  def handleUpdateRelationshipEvent(id: EntityStore.ID)
}
