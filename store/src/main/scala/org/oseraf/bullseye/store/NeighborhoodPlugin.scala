package org.oseraf.bullseye.store

/**
 * Created by nhamblet.
 */
trait NeighborhoodPlugin {
  val store: EntityStore

  def neighborhood(entityId: EntityStore.ID): Iterable[EntityStore.ID]
}

trait NNeighborhoodPlugin {
  val store: EntityStore

  def nNeighborhood(entityId: EntityStore.ID, n: Int): (Set[EntityStore.ID], Set[(EntityStore.ID, String, EntityStore.ID)])
}