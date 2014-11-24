package org.oseraf.bullseye.store

/**
 * Created by nhamblet.
 */
trait NeighborhoodPlugin {
  val store: EntityStore

  def neighborhood(entityId: EntityStore.ID): Iterable[EntityStore.ID]
}
