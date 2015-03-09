package org.oseraf.bullseye.service.DataService

import org.oseraf.bullseye.store.{EntityStore, Entity}

/**
 * Created by sstyer on 3/9/15.
 */
trait Resolver {
  def deduplicate():Seq[BullsEyeDedupeCandidate]
  def resolve(entId:EntityStore.ID):Seq[BullsEyeEntityScore]
}
