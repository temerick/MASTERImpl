package org.oseraf.bullseye.service.DataService

import com.typesafe.scalalogging.slf4j.Logging
import no.priv.garshol.duke.Configuration
import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store.EntityStore.ID
import org.oseraf.bullseye.store._
import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Created by sstyer on 2/10/15.
 */
trait GraphContextResolver extends Service with Resolver {
  val duke:DukeResolver
  val store:EntityStore with NeighborhoodPlugin with EntityIterationPlugin

  def getDukeCandidateNeighborhoods(entId:EntityStore.ID):Seq[((EntityStore.ID, Double), Set[EntityStore.ID])] = {
    val ent = store.entity(entId)
    val dukeCandidates = duke.resolve(entId, Option((x:Double) => x >= duke.dukeConf.getMaybeThreshold))
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

  def getDocId(entId:EntityStore.ID):EntityStore.ID = Option(neighborhoodEntityIds(entId).filter(isDoc)) match {
      case Some(List()) => entId
      case Some(docIds) => docIds.head
      case None => entId //shouldn't really happen
  }

 override def resolve(entId:EntityStore.ID, filterOrAll:Option[Double => Boolean]=None):Seq[BullsEyeEntityScore] = {
   val entNeighborhood = {
     val docId = getDocId(entId)
      (docId, neighborhoodEntityIds(docId).toSet)
    }
    getDukeCandidateNeighborhoods(entId)
      .flatMap{
        case(((dukeCandidate, dukeScore), comentions)) => {
          filterOrAll match {
            case Some(predicate) => {
              compare(dukeScore, entNeighborhood, (dukeCandidate, comentions), predicate)
            }
            case None => {
              compare(dukeScore, entNeighborhood, (dukeCandidate, comentions))
            }
          }
        }
    }
  }

  def compare(candidateScore:Double, n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID]),
              predicate:Double => Boolean=(x:Double) => true):Option[BullsEyeEntityScore] = {
    val updatedScore = probUpdate(candidateScore, neighborhoodContextScore(n1, n2))
    predicate(updatedScore) match {
      case true => Some(BullsEyeEntityScore(toBullsEyeEntity(n2._1, store), updatedScore))
      case false => None
    }
  }

  def neighborhoodContextScore(n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID])):Double = jaccardIndex(n1._2, n2._2)
  def jaccardIndex(s1:Set[EntityStore.ID], s2:Set[EntityStore.ID]):Double = (s1 intersect s2).size / (s1 union s2).size
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

  def getThresholdDuplicates(resolver:Resolver, start:Double=0.65, step:Double=0.05, end:Double=1):Seq[(Double, Seq[(Seq[EntityStore.ID], Double)])] = {
      (for(i <- start to end by step) yield { i }).toSeq
        .map(i => {
          logger.info(s"($i, $resolver.deduplicate())")
            (i, {
              resolver.dukeConf.setThreshold(i)
              resolver.deduplicate(store)
                .map(bdc => (bdc.entities.toSeq.map(_.id), bdc.score))
            })
        }).seq
  }

  def numberDupsVsThreshold(resolver:Resolver):Map[Double, Int] =
    getThresholdDuplicates(resolver)
      .map{
        case(thresh:Double, candidates:Seq[(Seq[EntityStore.ID], Double)]) =>
          thresh -> candidates.size}.toMap

  def getGraphThresholdDuplicates(resolver:GraphContextResolver, start:Double=0.65, step:Double=0.05, end:Double=1):Seq[(Double, Seq[(Double, Seq[(Seq[EntityStore.ID], Double)])])] = {
    getThresholdDuplicates(resolver.duke, start, step, end)
     .map{
      case(thresh, candidates) =>
        (thresh, {
          getThresholdDuplicates(resolver, thresh)
        })
    }.seq
  }

  def graphNumberDupsVsThreshold(resolver:GraphContextResolver, store:EntityIterationPlugin):Map[(Double, Double), Int] = {
    getGraphThresholdDuplicates(resolver)
      .flatMap {
      case (thresh1:Double, threshDups:Seq[(Double, Seq[(Seq[EntityStore.ID], Double)])]) => {
        threshDups.map{
          case(thresh2:Double, dups:Seq[(Seq[EntityStore.ID], Double)]) => {
            (thresh1, thresh2) -> dups.size
          }
        }
      }
    }.toMap
  }

  def entityDiff(resolver:Resolver, store:EntityIterationPlugin, thresh1:Double, thresh2:Double) = {
    val thresholdCandidates = getThresholdDuplicates(resolver, thresh1, thresh2-thresh1, thresh2).toMap
    thresholdCandidates(thresh1).toSet diff thresholdCandidates(thresh2).toSet
  }
}

trait Resolver {
  val dukeConf:Configuration
  def resolve(entId:EntityStore.ID, filters:Option[Double => Boolean]=None):Seq[BullsEyeEntityScore]
  def toBullsEyeEntity(entId:EntityStore.ID, store:EntityIterationPlugin) = {
    BullsEyeEntity(entId)
  }
  def deduplicate(store:EntityIterationPlugin): Seq[BullsEyeDedupeCandidate] = {
    var cnt = -1
    store
      .entities
       .flatMap(entityId => {
         resolve(entityId, Option((x:Double) => x >= dukeConf.getThreshold))
          .map(dupe =>
            BullsEyeDedupeCandidate(
              Seq(toBullsEyeEntity(entityId, store), dupe.entity).toSet,
              dupe.score
            )
          )
    }).toSet.seq.toSeq
  }
}

