package org.oseraf.bullseye.service.DataService

import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store.{BinaryRelationship, NeighborhoodPlugin, EntityStore, Entity}
import scala.collection.mutable
/**
 * Created by sstyer on 2/10/15.
 */
trait GraphMatchingResolver extends Service {
  val duke:DukeResolver
  val entityStore:BlueprintsBullseyeEntityStore with NeighborhoodPlugin

  def getNeighborHoodIds(entId:EntityStore.ID):Map[EntityStore.ID, Seq[EntityStore.ID]] = {
    val ent = entityStore.entity(entId)
    val dukeCandidates = duke.resolve(entId).map(_.entity)
    val docIds = new mutable.HashSet[EntityStore.ID]
    dukeCandidates.map(dukeCandidateEntity => {
      val docId = entityStore.relationship(dukeCandidateEntity, label="mentionedIn").asInstanceOf[BinaryRelationship].to
      if(!docIds.contains(docID)) {
        docIds += docId
        (docId, entityStore.neighborhood(docId)
          .map(relId => entityStore.relationship(relId).asInstanceOf[BinaryRelationship].to))
      }
    }).toMap
  }

  def resolve(entId:EntityStore.ID) = {
      }

  def neighborHoodScore(n1:(EntityStore.ID, Seq[EntityStore.ID]), n2:(EntityStore.ID, Seq[EntityStore.ID])):Double = {
    0.0
  }
}
