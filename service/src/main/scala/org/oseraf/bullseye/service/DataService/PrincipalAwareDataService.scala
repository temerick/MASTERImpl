package org.oseraf.bullseye.service.DataService

import org.oseraf.bullseye.store.{Entity, EntityStore}
import java.security.Principal

trait PrincipalAwareDataService extends DataService {

  final val AUTH_KEY = "OSERAF:auth/basic/owner"

  def search(p:Principal, query:String, searchTypeId:String, limit:Option[Int]): ScoredBullsEyeGraph = {
    val scGraph = super.search(query, searchTypeId, limit)
    filterGraph(p, scGraph)
  }
  def resolve(p:Principal, targetEntityId: EntityStore.ID, limit:Option[Int]) = {
    getBullsEyeEntityDetails(p, targetEntityId) match {
      case Some(ent) => super.resolve(targetEntityId, limit).filter(scEnt => isOK(p, scEnt.entity))
      case _ => List()
    }
  }
  //def deduplicate(p:Principal) = super.deduplicate.filter(cand => cand.entities.forall(ent => isOK(p, ent)))

  def getNeighborhood(p:Principal, entityId: EntityStore.ID) : BullsEyeGraph = {
    getBullsEyeEntityDetails(p, entityId) match {
      case Some(ent) => {
        val neighborhood = super.getNeighborhood(entityId)
        filterGraph(p, neighborhood)
      }
      case _ => BullsEyeGraph(List(), List())
    }
  }
  def getBullsEyeEntityDetails(p:Principal, entityId: EntityStore.ID) : Option[BullsEyeEntity] = {
    val ent = super.getBullsEyeEntityDetails(entityId)
    if(isOK(p, ent)) Some(ent.copy(edges=filterEdgesFrom(p, ent)))
    else None
  }
  private def filterGraph(p:Principal, g:BullsEyeGraph) = {
    val nodes = g.nodes.filter(ent => isOK(p, ent))
    val filteredEdgeNodes:Seq[BullsEyeEntity] = nodes.map(node => node.copy(edges=filterEdgesFrom(p, node)))
    val nodeIds = filteredEdgeNodes.map(_.id)
    val edges = g.edges.filter(edge => nodeIds.contains(edge.source) && nodeIds.contains(edge.target))
    BullsEyeGraph(filteredEdgeNodes, edges)
  }
  private def filterGraph(p:Principal, g:ScoredBullsEyeGraph) = {
    val scNodes = g.nodes.filter(scEnt => isOK(p, scEnt.entity))
    val filteredEdgeNodes:Seq[BullsEyeEntityScore] = scNodes.map(node => node.copy(entity=node.entity.copy(edges=filterEdgesFrom(p, node.entity))))
    val nodeIds = filteredEdgeNodes.map(_.entity.id)
    val edges = g.edges.filter(edge => nodeIds.contains(edge.source) && nodeIds.contains(edge.target))
    ScoredBullsEyeGraph(filteredEdgeNodes, edges)
  }
  private def filterEdgesFrom(p:Principal, entity:BullsEyeEntity):Seq[BullsEyeEdge] = {
    entity.edges.filter(edge => getBullsEyeEntityDetails(p, edge.source).isDefined && getBullsEyeEntityDetails(p, edge.target).isDefined)
  }
  private def isOK(p:Principal, entity:Entity):Boolean = {
    entity.attributes.get(AUTH_KEY) match {
      case Some(pName) => pName == p.getName
      case _ => true //there is no Authorization key/value so this entity is globally available
    }
  }
  def merge(p:Principal, entityIds: Seq[EntityStore.ID], mergedEntity:BullsEyeEntity): BullsEyeEntity = {
    val visibleEdgeFilteredEntities = entityIds.flatMap(eId => getBullsEyeEntityDetails(p, eId))
    val newMergedEntity = mergedEntity.copy(attrs=mergedEntity.attrs + (AUTH_KEY -> p.getName))
    super.merge(visibleEdgeFilteredEntities.map(_.id), newMergedEntity)
  }
  def split(p:Principal, entityId: EntityStore.ID, splitEntities:Seq[BullsEyeEntity]): Seq[BullsEyeEntity] = {
    getBullsEyeEntityDetails(p, entityId) match {
      case Some(ent) => {
        val newSplitEntities = splitEntities.map(e => e.copy(attrs=e.attrs + (AUTH_KEY -> p.getName)))
        super.split(entityId, newSplitEntities)
      }
      case _ => List()
    }
  }
}
