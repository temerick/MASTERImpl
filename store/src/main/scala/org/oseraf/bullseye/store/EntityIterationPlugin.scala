package org.oseraf.bullseye.store

/**
 * Created by nhamblet.
 */
trait EntityIterationPlugin {
  def entities: Iterable[EntityStore.ID]
}

trait RelationshipIterationPlugin {
  def relationships: Iterable[EntityStore.ID]
}