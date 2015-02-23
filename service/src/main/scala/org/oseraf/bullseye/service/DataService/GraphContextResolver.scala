package org.oseraf.bullseye.service.DataService

import com.typesafe.scalalogging.slf4j.Logging
import no.priv.garshol.duke.Configuration
import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store.EntityStore.ID
import org.oseraf.bullseye.store._
import scala.collection.JavaConversions._
import scala.collection.mutable
import java.util.TreeMap

/**
 * Created by sstyer on 2/10/15.
 */
trait GraphContextResolver extends Service with Resolver {
  val duke:DukeResolver
  val store:EntityStore with NeighborhoodPlugin with EntityIterationPlugin

  def getDukeCandidateNeighborhoods(entId:EntityStore.ID, dukeThresh:Double):Seq[((EntityStore.ID, Double), Set[EntityStore.ID])] = {
    val ent = store.entity(entId)
    val dukeCandidates = duke.resolve(entId, dukeThresh)
    dukeCandidates
      .map(dukeCandidate => {
        val docId = getDocId(dukeCandidate.entity.id)
        ((dukeCandidate.entity.id, dukeCandidate.score), neighborhoodEntityIds(docId).toSet)
    })
  }
  def neighborhoodEntityIds(entId:EntityStore.ID):Seq[EntityStore.ID] =
    store.neighborhood(entId).toSeq
      .map(relId => store.relationship(relId)
       .connecting()
        .filter(id => id != entId).head)

  def isDoc(entId:EntityStore.ID) = store.entity(entId).attributes.contains("title")

  def getDocId(entId:EntityStore.ID):EntityStore.ID = neighborhoodEntityIds(entId).filter(isDoc).headOption.getOrElse(entId)

  override def resolve(entId:EntityStore.ID, dukeThresh:Double):Seq[BullsEyeEntityScore] = {
   val entNeighborhood = {
     val docId = getDocId(entId)
      (docId, neighborhoodEntityIds(docId).toSet)
    }
    getDukeCandidateNeighborhoods(entId, dukeThresh)
      .flatMap{
        case(((dukeCandidate, dukeScore), comentions)) => {
          compare(dukeScore, entNeighborhood, (dukeCandidate, comentions), dukeThresh)
        }
    }
  }

  def compare(candidateScore:Double, n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID]), dukeThresh:Double=0):Option[BullsEyeEntityScore] = {
    val updatedScore = probUpdate(candidateScore, neighborhoodContextScore(n1, n2))
//    logger.info("contextScore: " + neighborhoodContextScore(n1, n2))
    updatedScore >= dukeThresh match {
      case true => Some(BullsEyeEntityScore(toBullsEyeEntity(n2._1, store), updatedScore))
      case false => None
    }
  }

  def neighborhoodContextScore(n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID])):Double = {
    jaccardIndex(
      n1._2.map(entId => store.entity(entId).attributes("actual_name")),
      n2._2.map(entId => store.entity(entId).attribute("actual_name")))
  }
  def jaccardIndex(s1:Set[EntityStore.ID], s2:Set[EntityStore.ID]):Double = (s1 intersect s2).size * 1.0 / (s1 union s2).size * 1.0
  def probUpdate(oldP:Double, newP:Double):Double = oldP + (1 - oldP)*newP
}

trait Evaluator {
  //def getDukeInfo = Map("properties" -> dukeConf.getProperties, "threshold" -> dukeConf.getThreshold)
  //def getComparators = dukeConf.getProperties.map(_.getComparator.toString)
}

trait ScoreEvaluator extends Evaluator with Logging {
  val store:EntityStore with NeighborhoodPlugin with EntityIterationPlugin

  def getAttributes():Set[AttributeContainer.KEY] =
    store.entities
      .flatMap(eId =>
        store.entity(eId)
          .attributes.keys).toSet

  def distinctValues(col:AttributeContainer.KEY):Set[AttributeContainer.VALUE] =
    store.entities
      .map(eId => store
       .entity(eId)
        .attribute(col)).toSet

  def degreeDistribution():Map[Int,Int] = {
    var degreeCounts = new mutable.HashMap[Int, Int]
    for (eId <- store.entities) yield {
      val neighborhoodSize = store.neighborhood(eId).size
      degreeCounts.get(neighborhoodSize) match {
        case Some(int) => degreeCounts.put(neighborhoodSize, int + 1)
        case None =>  degreeCounts.put(neighborhoodSize, 1)
      }
    }
    degreeCounts.toSeq.sortBy(_._1).toMap
  }

  def numberDupsVsThreshold(resolver:Resolver, thresh:Double=0, start:Int=65, step:Int=5, end:Int=100):Map[Int, Int] = {
    var threshMap = new TreeMap[Int, Int]
    var ents = new mutable.HashSet[EntityStore.ID]
    for (i <- start to end by step) threshMap.put(i, 0)
    resolver.deduplicate(store, thresh)
      .foreach(bdc => {
      val entId = bdc.entities.head.id
      if(bdc.score >= 0.65 && !ents.contains(entId)) {
        val k = ((bdc.score * 20).toInt / 20.0) * 100 toInt
        val v = threshMap.get(k)
        threshMap.put(k, v + 1)
        ents += (bdc.entities.head.id)
      }
    })
    threshMap.toMap
  }
}

trait Resolver {
  val dukeConf:Configuration
  def resolve(entId:EntityStore.ID, thresh:Double):Seq[BullsEyeEntityScore]
  def toBullsEyeEntity(entId:EntityStore.ID, store:EntityStore with EntityIterationPlugin) = {
    UiConverter.EntityToBullsEyeEntity(entId, store.entity(entId))
  }
  def deduplicate(store:EntityStore with EntityIterationPlugin, thresh:Double=0): Seq[BullsEyeDedupeCandidate] = {
    var cnt = -1
    store
      .entities
       .flatMap(entityId => {
         resolve(entityId, thresh)
          .map(dupe =>
            BullsEyeDedupeCandidate(
              Seq(toBullsEyeEntity(entityId, store), dupe.entity).toSet,
              dupe.score
            )
          )
    }).toSet.seq.toSeq
  }
}

