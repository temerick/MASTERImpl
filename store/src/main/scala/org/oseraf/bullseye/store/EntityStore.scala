package org.oseraf.bullseye.store

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import org.oseraf.bullseye.store.EntityStore.ID
import org.oseraf.bullseye.store.AttributeContainer.{KEY,VALUE}

/**
 * Created by nhamblet.
 */
trait EntityStore {
  def entity(id: EntityStore.ID): Entity
  def relationship(id: EntityStore.ID): Relationship

  def identifiedEntity(id: EntityStore.ID): IdentifiedEntity = IdentifiedEntity(id, entity(id))

  def addEntity(id: EntityStore.ID, entity: Entity): Boolean
  def addRelationship(id: EntityStore.ID, relationship: Relationship): Boolean

  def addEntity(entity: Entity): Option[EntityStore.ID] = {
    val id = idGenerator(entity)
    if (addEntity(id, entity)) Some(id)
    else None
  }
  def addRelationship(relationship: Relationship): Option[EntityStore.ID] = {
    val id = idGenerator(relationship)
    if (addRelationship(id, relationship)) Some(id)
    else None
  }

  def removeEntity(id: EntityStore.ID): Boolean
  def removeRelationship(id: EntityStore.ID): Boolean

  def updateEntity(id: EntityStore.ID, entity: Entity): Boolean
  def updateRelationship(id: EntityStore.ID, relationship: Relationship): Boolean

  def entityExists(id: EntityStore.ID): Boolean
  def relationshipExists(id: EntityStore.ID): Boolean

  lazy val idGenerator = IdGenerator()
}

object EntityStore {
  type ID = String
}

trait IdGenerator {
  def apply(entity: Entity): EntityStore.ID = apply()
  def apply(relationship: Relationship): EntityStore.ID = apply()
  def apply(): EntityStore.ID
}

object IdGenerator {
  // provide a sensible default generator
  def apply(): IdGenerator = {
    new IdGenerator {
      // a sensible default uses inherently meaningless ids
      override def apply(): EntityStore.ID = UUID.randomUUID().toString
    }
  }

  def unsafeSequential(): IdGenerator = {
    new IdGenerator {
      var count = 0l
      override def apply(): ID = {
        val id = count.toString
        count += 1l
        id
      }
    }
  }

  def atomicSequential(): IdGenerator = {
    new IdGenerator {
      val counter = new AtomicLong()
      override def apply(): ID = {
        counter.getAndIncrement.toString
      }
    }
  }
}

trait PointedEntityStore extends EntityStore {
  val pointEntityID: EntityStore.ID
}


trait Identified {
  val id: EntityStore.ID
}

trait AttributeContainer {
  var attributes: Map[AttributeContainer.KEY, AttributeContainer.VALUE]
  def attribute(key: AttributeContainer.KEY): AttributeContainer.VALUE = attributes(key)
  def attribute(key: AttributeContainer.KEY, default: AttributeContainer.VALUE) = attributes.getOrElse(key, default)
  def hasAttribute(key: AttributeContainer.KEY) = attributes.contains(key)
}

object AttributeContainer {
  type KEY = String
  type VALUE = String

  def empty: AttributeContainer =
    new AttributeContainer {
      override var attributes = Map.empty[KEY, VALUE]
    }
}

trait Entity extends AttributeContainer

trait IdentifiedEntity extends Entity with Identified

object Entity {
  def apply(): Entity =
    new Entity {
     override var attributes = Map.empty[KEY, VALUE]
    }
}

object IdentifiedEntity {
  def apply(entityId: EntityStore.ID): IdentifiedEntity =
    new IdentifiedEntity {
      override var attributes = Map.empty[KEY, VALUE]
      val id = entityId
    }
  
  def apply(entityId: EntityStore.ID, entity: Entity): IdentifiedEntity =
    new IdentifiedEntity {
      override var attributes = entity.attributes
      override val id = entityId 
    }
}

trait Relationship extends AttributeContainer {
  def connecting(): Iterable[EntityStore.ID]
}

trait BinaryRelationship extends Relationship {
  def from: EntityStore.ID
  def to: EntityStore.ID
  def isDirected: Boolean
  override def connecting() = Seq(from, to)
}
