package org.oseraf.bullseye.service.DataService

import java.util.UUID

import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import org.oseraf.bullseye.store.AttributeContainer.{KEY, VALUE}
import org.oseraf.bullseye.store.EntityStore.ID
import org.oseraf.bullseye.store._
import org.oseraf.bullseye.store.impl.blueprints.BlueprintsGraphStore

object UiConverter {

  def RelationshipToBullsEyeEdge(relationship: Relationship) = {
    val incidentals = relationship.connecting().toArray
    BullsEyeEdge(
      incidentals(0),
      incidentals(1),
      relationship.attributes
    )
  }

  def RelationshipsToBullsEyeEdges(relationships: Seq[Relationship]): Seq[BullsEyeEdge] =
    relationships.map(RelationshipToBullsEyeEdge)

  def EntityToBullsEyeEntity(entity: IdentifiedEntity, edges: Seq[Relationship] = Seq()): BullsEyeEntity =
    BullsEyeEntity(
      entity.id,
      entity.attributes,
      RelationshipsToBullsEyeEdges(edges)
    )

  def EntityToBullsEyeEntity(entityId: String, entity: Entity): BullsEyeEntity =
    BullsEyeEntity(
      entityId,
      entity.attributes,
      Seq()
    )

  def EntitiesToBullsEyeEntities(entities: Seq[IdentifiedEntity]): Seq[BullsEyeEntity] =
    entities.map(ent => EntityToBullsEyeEntity(ent))

  def EntityWithScoreToBullsEyeEntityScore(entityScore: (IdentifiedEntity, Double)): BullsEyeEntityScore =
    BullsEyeEntityScore(EntityToBullsEyeEntity(entityScore._1), entityScore._2)

  def EntitiesWithScoresToBullsEyeEntityScore(entitiesWithScores: Seq[(IdentifiedEntity, Double)]): Seq[BullsEyeEntityScore] =
    entitiesWithScores.map(EntityWithScoreToBullsEyeEntityScore)


  def AddJustBullseyeEntityToStore(entity: BullsEyeEntity, store: EntityStore) = {
    if (!store.entityExists(entity.id)) {
      store.addEntity(entity.id, new Entity {
        override var attributes = entity.attrs
      })
    }
  }

  def UpdateAttributesForBullseyeEntityInStore(entity: BullsEyeEntity, store: EntityStore) = {
    store.entity(entity.id).attributes = entity.attrs
  }

  def AddBullseyeEntityWithNeighborhood(entity: BullsEyeEntity, store: EntityStore) = {
    AddJustBullseyeEntityToStore(entity, store)
    UpdateAttributesForBullseyeEntityInStore(entity, store)
    for (edge <- entity.edges) {
      val alter = if (edge.source == entity.id) edge.target else edge.source
      AddJustBullseyeEntityToStore(BullsEyeEntity(alter), store)
      addRelationship(store, edge.source, edge.target, edge.attrs)
    }
  }

  def AddBullseyeEdgeToStore(edge: BullsEyeEdge, store: EntityStore) = {
    addRelationship(store, edge.source, edge.target, edge.attrs)
  }

  def addRelationship(store: EntityStore, fromId: ID, toId: ID, attrs: Map[KEY, VALUE] = Map.empty) = {
    store.addRelationship(
      UUID.randomUUID().toString,
      new BinaryRelationship {
        override def isDirected = true
        override def from = fromId
        override def to = toId
        override var attributes = attrs
      }
    )
  }

  def BullseyeGraphToBlueprintsGraphStore(graph: BullsEyeGraph): BlueprintsGraphStore = {
    val store = BlueprintsGraphStore(new TinkerGraph())
    graph.nodes.foreach(ent => AddBullseyeEntityWithNeighborhood(ent, store))
    graph.edges.foreach(edge => AddBullseyeEdgeToStore(edge, store))
    store
  }

  def BullseyeEntityToBlueprintsGraphStore(entity: BullsEyeEntity): BlueprintsGraphStore = {
    val store = BlueprintsGraphStore(new TinkerGraph())
    AddBullseyeEntityWithNeighborhood(entity, store)
    store
  }
}
