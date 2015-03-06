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
case class GraphBullsEyeDedupeCandidate (entities:Seq[BullsEyeEntity], score:Double, graphScore:Double)
case class GraphBullsEyeEntityScore(entity: BullsEyeEntity, score: Double, graphScore:Double)

trait GraphContextResolver extends Service {
  val dukeConf:Configuration
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

  def resolve(entId:EntityStore.ID, dukeThresh:Double=0, graphThresh:Double=0):Seq[GraphBullsEyeEntityScore] = {
   val entNeighborhood = {
     val docId = getDocId(entId)
      (docId, neighborhoodEntityIds(docId).toSet)
    }
    getDukeCandidateNeighborhoods(entId, dukeThresh)
      .flatMap{
        case(((dukeCandidate, dukeScore), comentions)) => {
          compare(dukeScore, entNeighborhood, (dukeCandidate, comentions), graphThresh)
        }
      }
  }

  def compare(candidateScore:Double, n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID]), graphThresh:Double=0):Option[GraphBullsEyeEntityScore] = {
    val updatedScore = probUpdate(candidateScore, neighborhoodContextScore(n1, n2))
    //    logger.info("contextScore: " + neighborhoodContextScore(n1, n2))
    updatedScore >= graphThresh match {
      case true => Some(GraphBullsEyeEntityScore(toBullsEyeEntity(n2._1, store), candidateScore, updatedScore))
      case false => None
    }
  }

  def deduplicate(store:EntityStore with EntityIterationPlugin, dukeThresh:Double=0, graphThresh:Double=0): Seq[GraphBullsEyeDedupeCandidate] = {
    var cnt = -1
    store
      .entities
        .flatMap(entityId => {
          resolve(entityId, dukeThresh, graphThresh)
            .map(dupe =>
              GraphBullsEyeDedupeCandidate(Seq(toBullsEyeEntity(entityId, store), dupe.entity).toSet.toSeq, dupe.score, dupe.graphScore))
    }).toSet.seq.toSeq
  }

  def toBullsEyeEntity(entId:EntityStore.ID, store:EntityStore with EntityIterationPlugin) = {
    UiConverter.EntityToBullsEyeEntity(entId, store.entity(entId))
  }

  def neighborhoodContextScore(n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID])):Double = {
    jaccardIndex(
      n1._2.map(entId => store.entity(entId).attributes("actual_name")),
      n2._2.map(entId => store.entity(entId).attributes("actual_name")))
  }
  def jaccardIndex(s1:Set[EntityStore.ID], s2:Set[EntityStore.ID]):Double = (s1 intersect s2).size * 1.0 / (s1 union s2).size * 1.0
  def probUpdate(oldP:Double, newP:Double):Double = oldP + (1 - oldP)*newP

//---------------------------------------------------------------------------------------------------------------------------------------
//  def getAttributes():Set[AttributeContainer.KEY] =
//    store.entities
//      .flatMap(eId =>
//        store.entity(eId)
//          .attributes.keys).toSet
//
//  def distinctValues(col:AttributeContainer.KEY):Set[AttributeContainer.VALUE] =
//    store.entities
//      .map(eId => store
//       .entity(eId)
//        .attribute(col)).toSet
//
//  def degreeDistribution():Map[Int,Int] = {
//    var degreeCounts = new mutable.HashMap[Int, Int]
//    for (eId <- store.entities) yield {
//      val neighborhoodSize = store.neighborhood(eId).size
//      degreeCounts.get(neighborhoodSize) match {
//        case Some(int) => degreeCounts.put(neighborhoodSize, int + 1)
//        case None =>  degreeCounts.put(neighborhoodSize, 1)
//      }
//    }
//    degreeCounts.toSeq.sortBy(_._1).toMap
//  }

  def numberDupsVsThreshold(resolver:DukeResolver, dukeThresh:Double=0, start:Int=65, step:Int=5, end:Int=100):Map[Int, Int] = {
    var threshMap = new mutable.HashMap[Int, Int]
    var ents = new mutable.HashSet[EntityStore.ID]
    for (i <- start to end by step) threshMap.put(i, 0)

    bullseyeDedupCandidates(resolver, 0)
      .foreach(bdc => {
        val entId = bdc.entities.head.id
        val dukeScore = toQuintInt(bdc.score)
        if(!ents.contains(entId) && dukeScore >= start) {
          val v = threshMap(dukeScore)
          threshMap.put(dukeScore, v + 1)
          ents += entId
        }
      })
    threshMap.toMap
  }

  def toQuintInt(dub:Double):Int = (((dub * 20).toInt / 20.0) * 100).toInt

  def numberGraphDupsVsDukeThresholds(resolver:GraphContextResolver, start:Int=65, step:Int=5, end:Int=100):mutable.HashMap[Int, mutable.HashMap[Int,Int]] = {
    var m = new mutable.HashMap[Int, mutable.HashMap[Int,Int]]
    for(i <- start to end by step) {
      var im = new mutable.HashMap[Int, Int]
      for(j <- start to end by step) {
        im.put(j, 0)
      }
      m.put(i, im)
    }
    var ents = new mutable.HashSet[EntityStore.ID]
    resolver.bullseyeGraphDedupCandidates(resolver, 0, 0)
      .foreach(gbdc => {
        val entId = gbdc.entities.head.id
        val dukeScore = toQuintInt(gbdc.score)
        val graphScore = toQuintInt(gbdc.graphScore)
        if(!ents.contains(entId) && dukeScore >= start && graphScore >= start) {
          val v = m(dukeScore)(graphScore)
          m(dukeScore)(graphScore) = v + 1
          ents += entId
        }
      })
    m
  }

  def resolutions(resolver:DukeResolver):Map[EntityStore.ID, Seq[BullsEyeEntityScore]] =
    store.entities.map(tarEnt =>
      tarEnt -> resolver.resolve(tarEnt)).toMap

  def filteredResolutions(resolver:DukeResolver, compareScore:Double=0):Map[EntityStore.ID, Seq[BullsEyeEntityScore]] =
    resolutions(resolver)
      .map{case(tarEnt, dupCandidates) =>
        tarEnt -> dupCandidates.filter(dc => dc.score >= compareScore)}

  def filteredResolutionPairs(resolver:DukeResolver, compareScore:Double):Seq[(EntityStore.ID, BullsEyeEntityScore)] =
    filteredResolutions(resolver, compareScore).toSeq
      .flatMap{
        case(tarEnt, dupCandidates) =>
          dupCandidates.map(aDup => (tarEnt, aDup))}

  def bullseyeDedupCandidates(resolver:DukeResolver, compareScore:Double=.80) =
    filteredResolutionPairs(resolver, compareScore)
      .map{
      case(tarEntId, gbdc) =>
        BullsEyeDedupeCandidate(
          Seq(
            toBullsEyeEntity(tarEntId, store),
            toBullsEyeEntity(gbdc.entity.id, store)), gbdc.score)}





  def resolutions(resolver:GraphContextResolver):Map[EntityStore.ID, Seq[GraphBullsEyeEntityScore]] =
    store.entities.map(tarEnt =>
      tarEnt -> resolver.resolve(tarEnt)).toMap

  def filteredResolutions(resolver:GraphContextResolver, score:Double, scoreDiff:Double):Map[EntityStore.ID, Seq[GraphBullsEyeEntityScore]] =
    resolutions(resolver)
      .map{case(tarEnt, dupCandidates) =>
        tarEnt -> dupCandidates.filter(gbdc =>
          gbdc.score >= score && (gbdc.graphScore - gbdc.score) >= scoreDiff)}

  def filteredResolutionPairs(resolver:GraphContextResolver, score:Double, scoreDiff:Double):Seq[(EntityStore.ID, GraphBullsEyeEntityScore)] =
    filteredResolutions(resolver, score, scoreDiff).toSeq
      .flatMap{
        case(tarEnt, dupCandidates) =>
          dupCandidates.map(aDup => (tarEnt, aDup))}

  def bullseyeGraphDedupCandidates(resolver:GraphContextResolver, score:Double=.80, scoreDiff:Double=.05) =
    filteredResolutionPairs(resolver, score, scoreDiff)
      .map{
        case(tarEntId, gbdc) =>
          GraphBullsEyeDedupeCandidate(
            Seq(
              toBullsEyeEntity(tarEntId, store),
              toBullsEyeEntity(gbdc.entity.id, store)), gbdc.score, gbdc.graphScore)}

//  def getDistinctScores(resolver:Resolver, thresh:Double=0, limit:Option[Int]=None):Seq[Double] = {
//    resolver.deduplicate(store, thresh).map{
//      case bdc:BullsEyeDedupeCandidate =>
//        bdc.score
//    }.toSet.toSeq.sorted
//  }
}



