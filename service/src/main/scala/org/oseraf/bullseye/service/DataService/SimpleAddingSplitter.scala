package org.oseraf.bullseye.service.DataService

import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import org.oseraf.bullseye.store.impl.blueprints.BlueprintsGraphStore
import org.oseraf.bullseye.store.{Entity, EntityStore, IdentifiedEntity, SplicePlugin}

trait SimpleAddingSplitter {

  val store: EntityStore with SplicePlugin

  def split(splitId: EntityStore.ID, newSplitNeighborhood: SplicePlugin.SpliceableStore): Seq[IdentifiedEntity] = {
    val fromStore = BlueprintsGraphStore(new TinkerGraph())

    fromStore.addEntity(splitId, Entity())
    if (!newSplitNeighborhood.entityExists(splitId)) {
      val toEntity = new Entity {
        override var attributes = Map(SplicePlugin.ATTRIBUTE_BEHAVIOR_KEY -> SplicePlugin.ATTRIBUTE_BEHAVIOR__IGNORE)
      }
      newSplitNeighborhood.addEntity(splitId, toEntity)
    }

    val resultingEntities =
      newSplitNeighborhood.entities.foldLeft(Seq[IdentifiedEntity]()) {
        case (newEnts, entityId) =>
          if (store.entityExists(entityId)) {
            if (!fromStore.entityExists(entityId)) {
              fromStore.addEntity(entityId, Entity())
            }
            if (!newSplitNeighborhood.entity(entityId).hasAttribute(SplicePlugin.ATTRIBUTE_BEHAVIOR_KEY)) {
              newSplitNeighborhood.entity(entityId).attributes += (
                SplicePlugin.ATTRIBUTE_BEHAVIOR_KEY -> SplicePlugin.ATTRIBUTE_BEHAVIOR__IGNORE
                )
            }
          }
          if (!store.entityExists(entityId)) {
            UiConverter.addRelationship(
              newSplitNeighborhood,
              entityId,
              splitId,
              Map(SimpleAddingSplitter.RELATIONSHIP_SPLIT_ATTR_KEY -> SimpleAddingSplitter.RELATIONSHIP_SPLIT_ATTR_VAL)
            )
          }
          if (store.entityExists(entityId)) newEnts
          else newEnts ++ Seq(IdentifiedEntity(entityId, newSplitNeighborhood.entity(entityId)))
      }

    store.splice(fromStore, newSplitNeighborhood)

    resultingEntities
  }
}

object SimpleAddingSplitter {
  final val RELATIONSHIP_SPLIT_ATTR_KEY = "OSERAF:split/simple/edgeType"
  final val RELATIONSHIP_SPLIT_ATTR_VAL = "splitFrom"
}

trait SimpleAddingSplitterIdentifier {
  def targetId(splitFromId: EntityStore.ID, targetSplitId: EntityStore.ID): EntityStore.ID =
    targetSplitId
}