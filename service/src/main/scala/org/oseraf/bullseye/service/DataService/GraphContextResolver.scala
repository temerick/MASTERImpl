package org.oseraf.bullseye.service.DataService

import no.priv.garshol.duke.Configuration
import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store._
import scala.collection.mutable

trait GraphContextResolver extends Resolver {
  val dukeConf:Configuration
  val duke:DukeResolver
  val store:EntityStore with NeighborhoodPlugin with EntityIterationPlugin

  def getDukeCandidateNeighborhoods(entId:EntityStore.ID):Seq[(BullsEyeEntityScore, Set[EntityStore.ID])] = {
    val ent = store.entity(entId)
    val dukeCandidates = duke.resolve(entId)
    dukeCandidates
      .map(dukeCandidate => {
        val docId = getDocId(dukeCandidate.entity.id)
        (BullsEyeEntityScore(dukeCandidate.entity, dukeCandidate.score), neighborhoodEntityIds(docId).toSet)
      })
  }
  def neighborhoodEntityIds(entId:EntityStore.ID):Seq[EntityStore.ID] =
    store.neighborhood(entId).toSeq
      .map(relId => store.relationship(relId)
       .connecting()
        .filter(id => id != entId).head)

  def isDoc(entId:EntityStore.ID) = store.entity(entId).attributes.contains("title")

  def getDocId(entId:EntityStore.ID):EntityStore.ID = neighborhoodEntityIds(entId).filter(isDoc).headOption.getOrElse(entId)

  override def resolve(entId:EntityStore.ID):Seq[BullsEyeEntityScore] = {
   val entNeighborhood = {
     val docId = getDocId(entId)
      (docId, neighborhoodEntityIds(docId).toSet)
    }
    getDukeCandidateNeighborhoods(entId)
      .flatMap{
        case((entityScore:BullsEyeEntityPairScore, comentions)) => {
          compare(entityScore.score.scorePair._1, entNeighborhood, (entityScore.entity.id, comentions))
        }
      }
  }

  def compare(candidateScore:Double, n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID])):Option[BullsEyeEntityScore] = {
    val updatedScore = probUpdate(candidateScore, neighborhoodContextScore(n1, n2))
    Some(BullsEyeEntityScore(toBullsEyeEntity(n2._1, store), BullsEyePairedScore((candidateScore, updatedScore))))
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

  def resolutions():Map[EntityStore.ID, Seq[BullsEyeEntityScore]] =
    store.entities.map(tarEnt =>
      tarEnt -> resolve(tarEnt)).toMap

  def resolutionPairs():Seq[(EntityStore.ID, BullsEyeEntityScore)] =
    resolutions()
      .flatMap{
        case(tarEnt, dupCandidates) =>
          dupCandidates.map(dup =>
            (tarEnt, dup))}.toSeq

//  def filteredResolutions(score:Double, scoreDiff:Double):Map[EntityStore.ID, Seq[BullsEyeEntityScore]] =
//    resolutions()
//      .map{case(tarEnt, dupCandidates) =>
//        tarEnt -> dupCandidates.filter(gbdc =>
//          gbdc.score >= score && (gbdc.graphScore - gbdc.score) >= scoreDiff)}

//  def filteredResolutionPairs(score:Double, scoreDiff:Double):Seq[(EntityStore.ID, BullsEyeEntityScore)] =
//    filteredResolutions(score, scoreDiff).toSeq
//      .flatMap{
//        case(tarEnt, dupCandidates) =>
//          dupCandidates.map(aDup => (tarEnt, aDup))}

  override def deduplicate =
    resolutionPairs()
      .map{
        case(tarEntId, gbdc) =>
          BullsEyeDedupeCandidate(
            Seq(
              toBullsEyeEntity(tarEntId, store),
              toBullsEyeEntity(gbdc.entity.id, store)), gbdc.score)}


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

//  def getDistinctScores(resolver:Resolver, thresh:Double=0, limit:Option[Int]=None):Seq[Double] = {
//    resolver.deduplicate(store, thresh).map{
//      case bdc:BullsEyeDedupeCandidate =>
//        bdc.score
//    }.toSet.toSeq.sorted
//  }
}



