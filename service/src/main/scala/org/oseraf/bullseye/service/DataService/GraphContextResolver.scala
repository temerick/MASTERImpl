package org.oseraf.bullseye.service.DataService

import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store._

/**
 * Created by sstyer on 2/10/15.
 */
trait GraphContextResolver extends Service {
  val duke:DukeResolver
  val store:BullseyeEntityStore

  def getNeighborhoods(entId:EntityStore.ID):Seq[((EntityStore.ID, Double), Set[EntityStore.ID])] = {
    val ent = store.entity(entId)
    val dukeCandidates = duke.resolve(entId)
    dukeCandidates.map{dukeCandidate => {
      val docId = getDocId(dukeCandidate.entity.id)
      ((dukeCandidate.entity.id, dukeCandidate.score), neighborhoodEntityIds(docId).toSet)
    }}
  }
  def neighborhoodEntityIds(entId:EntityStore.ID):Seq[EntityStore.ID] = {
    store.neighborhood(entId).toSeq
      .map(relId => store.relationship(relId)
       .connecting()
        .filter(id => id != entId).head)
  }
  def isDoc(entId:EntityStore.ID) = {
    store.entity(entId).attributes.contains("title")
  }
  def getDocId(entId:EntityStore.ID):EntityStore.ID = {
    Option(neighborhoodEntityIds(entId).filter(isDoc)) match {
      case Some(List()) => entId
      case Some(docIds) => docIds.head
      case None => entId //shouldn't really happen
    }
  }
  def resolve(entId:EntityStore.ID, limit:Option[Int]=None):Seq[BullsEyeEntityScore] = {
    val entNeighborhood = {
      val docId = getDocId(entId)
      (docId, neighborhoodEntityIds(docId).toSet)
    }
    getNeighborhoods(entId)
      .map{case(((dukeCandidate, dukeScore), comentions)) =>
      BullsEyeEntityScore(toBullsEyeEntity(dukeCandidate), probUpdate(dukeScore, neighborhoodContextScore(entNeighborhood, (dukeCandidate, comentions))))}
  }

  def neighborhoodContextScore(n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID])):Double = jaccardIndex(n1._2, n2._2)
  def jaccardIndex(s1:Set[EntityStore.ID], s2:Set[EntityStore.ID]):Double = (s1 intersect s2).size / (s1 union s2).size
  def probUpdate(oldP:Double, newP:Double):Double = oldP + (1 - oldP)*newP
  def toBullsEyeEntity(entId:EntityStore.ID) = BullsEyeEntity(entId, store.entity(entId).attributes)
  def deduplicate(): Seq[BullsEyeDedupeCandidate] = {
    var cnt = -1
      store.entities
      .flatMap(entityId => {
      resolve(entityId)
        .map(dupe =>
        BullsEyeDedupeCandidate(
          Seq(toBullsEyeEntity(entityId), dupe.entity).toSet,
          dupe.score
        )
        )
    }).toSet.seq.toSeq
  }
}

//trait Resolver {
//  val store:EntityStore with EntityIterationPlugin
//
//  def resolve(entId:EntityStore.ID, limit:Option[Int]=None):Seq[BullsEyeEntityScore]
//
//  def deduplicate(): Seq[BullsEyeDedupeCandidate] = {
//    var cnt = -1
//    store
//      .entities
//      .flatMap(entityId => {
//        resolve(entityId)
//          .map(dupe =>
//            BullsEyeDedupeCandidate(
//              Seq(toBullsEyeEntity(entityId), dupe.entity).toSet,
//              dupe.score
//            )
//          )
//    }).toSet.seq.toSeq
//  }
//}

