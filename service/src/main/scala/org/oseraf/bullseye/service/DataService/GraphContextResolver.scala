package org.oseraf.bullseye.service.DataService

import com.typesafe.scalalogging.slf4j.Logging
import no.priv.garshol.duke.Configuration
import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store._
import scala.collection.mutable

/**
 * Created by sstyer on 2/10/15.
 */
trait GraphContextResolver extends Service with Resolver {
  val duke:DukeResolver
  val store:EntityStore with NeighborhoodPlugin with EntityIterationPlugin

  def getNeighborhoods(entId:EntityStore.ID):Seq[((EntityStore.ID, Double), Set[EntityStore.ID])] = {
    val ent = store.entity(entId)
    val dukeCandidates = duke.resolve(entId, Option((x:Double) => x >= duke.dukeConf.getMaybeThreshold))
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
  def resolve(entId:EntityStore.ID, filterOrAll:Option[Double => Boolean]=None):Seq[BullsEyeEntityScore] = {
    val entNeighborhood = {
      val docId = getDocId(entId)
      (docId, neighborhoodEntityIds(docId).toSet)
    }
    getNeighborhoods(entId).flatMap{case(((dukeCandidate, dukeScore), comentions)) => {
      filterOrAll match {
        case Some(predicate) => compare(dukeScore, entNeighborhood, (dukeCandidate, comentions), predicate)
        case None => compare(dukeScore, entNeighborhood, (dukeCandidate, comentions))
      }
    }}
  }
  def compare(candidateScore:Double, n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID]), predicate:Double => Boolean=(x:Double) => true) = {
    val updatedScore = probUpdate(candidateScore, neighborhoodContextScore(n1, n2))
    predicate(updatedScore) match {
        case true => Some(BullsEyeEntityScore(toBullsEyeEntity(n1._1), updatedScore))
        case false => None
      }
  }

  def neighborhoodContextScore(n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID])):Double = jaccardIndex(n1._2, n2._2)
  def jaccardIndex(s1:Set[EntityStore.ID], s2:Set[EntityStore.ID]):Double = (s1 intersect s2).size / (s1 union s2).size
  def probUpdate(oldP:Double, newP:Double):Double = oldP + (1 - oldP)*newP

  override def deduplicate(): Seq[BullsEyeDedupeCandidate] = {
    var cnt = -1
      store.entities
      .flatMap(entityId => {
        resolve(entityId, Option((x:Double) => x >= duke.dukeConf.getThreshold))
        .map(dupe =>
          BullsEyeDedupeCandidate(
            Seq(toBullsEyeEntity(entityId), dupe.entity).toSet,
            dupe.score
          )
        )
    }).toSet.seq.toSeq
  }
}
trait Evaluator {
  val dukeConf:Configuration
  def getDukeInfo = {

  }
}
trait ScoreEvaluator extends Evaluator with Logging {
  val gresolver:Resolver


  def evaluate(step:Double=0.05):Seq[(Double, Seq[(Seq[EntityStore.ID], Double)])] = {
    var initThresh = 0.5
    for(i <- initThresh to 1 by step) yield {
      logger.info(s"($i, $gresolver.deduplicate())")
      (i,
        {
          dukeConf.setThreshold(i)
          gresolver.deduplicate()
        }
          .map(bdc => (bdc.entities.toSeq.map(_.id), bdc.score))
        )
    }
  }
  def numberVsThreshold():Seq[(Double, Int)] = {
    evaluate()
      .map{case(thresh, candidates) => (thresh, candidates.length)}
  }
}

trait Resolver {
  val store:EntityStore with EntityIterationPlugin
  def resolve(entId:EntityStore.ID, filters:Option[Double => Boolean]=None):Seq[BullsEyeEntityScore]
  def toBullsEyeEntity(entId:EntityStore.ID) = BullsEyeEntity(entId, store.entity(entId).attributes)
  def deduplicate(): Seq[BullsEyeDedupeCandidate] = {
    var cnt = -1
    store
      .entities
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

