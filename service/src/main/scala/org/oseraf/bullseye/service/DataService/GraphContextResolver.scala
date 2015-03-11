package org.oseraf.bullseye.service.DataService

import no.priv.garshol.duke.Configuration
import org.oseraf.bullseye.store._

trait GraphContextResolver extends Resolver {
  val dukeConf:Configuration
  val duke:DukeResolver
  val store:EntityStore with NeighborhoodPlugin with EntityIterationPlugin
  override type S = BullsEyePairedScore

  def getDukeCandidateNeighborhoods(entId:EntityStore.ID):Seq[((EntityStore.ID, Double), Set[EntityStore.ID])] = {
    val ent = store.entity(entId)
    val dukeCandidates = duke.resolve(entId)
    dukeCandidates
      .map(dukeCandidate => {
        val docId = getDocId(dukeCandidate._1)
        (((dukeCandidate._1, dukeCandidate._2.score), neighborhoodEntityIds(docId).toSet))
      })
  }

  def neighborhoodEntityIds(entId:EntityStore.ID):Seq[EntityStore.ID] =
    store.neighborhood(entId).toSeq
      .map(relId => store.relationship(relId)
       .connecting()
        .filter(id => id != entId).head)

  def isDoc(entId:EntityStore.ID) = store.entity(entId).attributes.contains("title")

  def getDocId(entId:EntityStore.ID):EntityStore.ID = neighborhoodEntityIds(entId).filter(isDoc).headOption.getOrElse(entId)

  override def resolve(entId:EntityStore.ID):Seq[(EntityStore.ID, BullsEyePairedScore)] = {
   val entNeighborhood = {
     val docId = getDocId(entId)
      (docId, neighborhoodEntityIds(docId).toSet)
    }
    getDukeCandidateNeighborhoods(entId)
      .flatMap{
        case((eId, score), comentions) => {
          compare(score, entNeighborhood, (eId, comentions))
        }
      }
  }

  def compare(candidateScore:Double, n1:(EntityStore.ID, Set[EntityStore.ID]), n2:(EntityStore.ID, Set[EntityStore.ID])):Option[(EntityStore.ID, BullsEyePairedScore)] = {
    val updatedScore = probUpdate(candidateScore, neighborhoodContextScore(n1, n2))
    Some(n2._1, BullsEyePairedScore((candidateScore, updatedScore)))
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
}



