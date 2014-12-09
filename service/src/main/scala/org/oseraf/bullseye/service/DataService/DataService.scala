package org.oseraf.bullseye.service.DataService

import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import no.priv.garshol.duke._
import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store.impl.blueprints.BlueprintsGraphStore
import org.oseraf.bullseye.store.{Entity, EntityStore, IdentifiedEntity}

case class BullsEyeEntity(id: String, attrs: Map[String, String] = Map(), edges: Seq[BullsEyeEdge] = Seq()) extends Entity {
  var attributes = attrs
}
case class BullsEyeEntityScore(entity: BullsEyeEntity, score: Double)
//source and target are entity ids
case class BullsEyeEdge(source: String, target: String, attrs: Map[String, String] = Map())
case class BullsEyeGraph(nodes: Seq[BullsEyeEntity], edges: Seq[BullsEyeEdge])
case class ScoredBullsEyeGraph(nodes: Seq[BullsEyeEntityScore], edges: Seq[BullsEyeEdge])
case class BullsEyeSearchType(id: String, name: String)
case class BullsEyeDedupeCandidate(entities: Iterable[BullsEyeEntity], score: Double)

trait DataService extends Service {

  val entityStore: BullseyeEntityStore = {
    val classLoader = this.getClass.getClassLoader
    val storeConf = conf.getConfig("store")
    val storeClazz = classLoader.loadClass(storeConf.getString("clazz"))
    val store = storeClazz.newInstance().asInstanceOf[BullseyeEntityStore]
    store.setup(storeConf.getConfig("args"))
    store
  }
  val resolutionStore = entityStore.resolutionStore
  val mergeIdentifier = entityStore.mergeIdentifier
  val splitIdentifier = entityStore.splitIdentifier

  val uic = UiConverter
  val merger = new SimpleAddingMerger { override val store = entityStore.spliceStore }
  val splitter = new SimpleAddingSplitter { override val store = entityStore.spliceStore }
  val resolverConf: Configuration = ConfigLoader.load(conf.getConfig("duke").getString("confPath"))
  val resolver = new DukeResolver(resolutionStore, resolverConf)

  def resolve(targetEntityId: EntityStore.ID, limit:Option[Int] = None) : Seq[BullsEyeEntityScore] =
    resolver.resolve(targetEntityId, limit)

  def deduplicate(): Seq[BullsEyeDedupeCandidate] =
    resolver.deduplicate()

  /**
   * Find entities most similar to the specified query
   * @param query The query string describing the entity to be found
   * @param searchTypeId The id of the search type property to match the query string against
   * @param limit optional limit of number of entities returned, probably should be sorted by entity score
   * @return list of entities and their similarity scores
   */
  def search(query:String, searchTypeId:String, limit:Option[Int] = None): ScoredBullsEyeGraph = {
    val scoredEntities = entityStore.search(searchTypeId, query).map { case (id, score) =>
      (entityStore.identifiedEntity(id), score)
    }.toSeq

    val connectIds = scoredEntities.map(_._1.id).toSet
    val (entities, edges) = scoredEntities.foldLeft((Seq[BullsEyeEntityScore](), Seq[BullsEyeEdge]())) {
      case ((partialScores, partialEdges), (identifiedEntity, score)) =>
        val edges = entityStore.neighborhood(identifiedEntity.id).map(relId => entityStore.relationship(relId)).toSeq
        val entity = uic.EntityToBullsEyeEntity(identifiedEntity, edges)
        val connectEdges = entity.edges.filter(edge => connectIds.contains(edge.source) && connectIds.contains(edge.target))
        (partialScores ++ Seq(BullsEyeEntityScore(entity, score)), partialEdges ++ connectEdges)
    }
    ScoredBullsEyeGraph(entities, edges)
  }

  /**
   * merges n entities into one
   * though the mergedEntity has an id, we allow the store to override it
   * @param entityIds The ids of the entities to merge
   * @param mergedEntity The final merged result
   * @return The final merged result
   */
  def merge (entityIds: Seq[EntityStore.ID], mergedEntity:BullsEyeEntity): BullsEyeEntity = {
    val entToMerge = replaceId(mergedEntity, mergeIdentifier.targetId(entityIds, mergedEntity.id))
    merger.merge(entityIds, uic.BullseyeEntityToBlueprintsGraphStore(entToMerge), entToMerge.id)
    expandEntity(entToMerge.id)
  }

  /**
   * split an entity into n entities
   * like merge, though the new entities have ids specified, we allow the store to override them
   * @param entityId id of entity to split
   * @param splitEntities The resulting n entities
   * @return the resulting n entities
   */
  def split (entityId: EntityStore.ID, splitEntities:Seq[BullsEyeEntity]): Seq[BullsEyeEntity] = {
    val newEntities = splitEntities.map(ent =>
      replaceId(ent, splitIdentifier.targetId(entityId, ent.id))
    )
    val store = BlueprintsGraphStore(new TinkerGraph())
    newEntities.foreach(ent => uic.AddBullseyeEntityWithNeighborhood(ent, store))
    // don't trust that the entity hasn't changed
    // split (/splice) may update attributes, for example, so we have to look the entity back up when we expand it
    splitter.split(entityId, store).map(ent => expandEntity(ent.id))
  }

  private def replaceId(ent: BullsEyeEntity, newId: EntityStore.ID): BullsEyeEntity = {
    val oldId = ent.id
    if (oldId == newId) ent
    else {
      def replace(x: String) = if (x == oldId) newId else x
      BullsEyeEntity(
        newId,
        ent.attrs,
        ent.edges.map { case edge =>
          BullsEyeEdge(replace(edge.source), replace(edge.target), edge.attrs)
        }
      )
    }
  }

  /**
   * Returns the full entity of the given id
   * @param entityId id of entity to get
   * @return entity with the passed id
   */
  def getBullsEyeEntityDetails(entityId: EntityStore.ID) : BullsEyeEntity =
    uic.EntityToBullsEyeEntity(entityStore.identifiedEntity(entityId))

  /**
   * Returns any entities that are linked to the current entity
   * @param entityId entity to find links to
   * @return graph that shows all connected links
   */
  def getNeighborhood(entityId: EntityStore.ID) : BullsEyeGraph = {
    val nodeIdsEdges = entityStore.neighborhood(entityId).foldLeft((Set[EntityStore.ID](), Seq[BullsEyeEdge]())) {
      case (graphSoFar, relId) =>
        val relationship = entityStore.relationship(relId)
        val edge = uic.RelationshipToBullsEyeEdge(relationship)
        (graphSoFar._1 ++ relationship.connecting(), graphSoFar._2 ++ Seq(edge))
    }
    BullsEyeGraph(
      nodeIdsEdges._1.map(eid => uic.EntityToBullsEyeEntity(eid, entityStore.entity(eid))).toSeq,
      nodeIdsEdges._2
    )
  }

  def bullseyeSearchTypes : Seq[BullsEyeSearchType] =
    entityStore.searchableAttributes.toSeq.map(searchAttr => {
      BullsEyeSearchType(searchAttr._1, searchAttr._2)
    })

  private def expandEntity(entityId: EntityStore.ID): BullsEyeEntity =
    expandEntity(entityStore.identifiedEntity(entityId))

  private def expandEntity(entity: IdentifiedEntity): BullsEyeEntity = {
    val relationships = entityStore.neighborhood(entity.id).map(rid => entityStore.relationship(rid))
    uic.EntityToBullsEyeEntity(entity, relationships.toSeq)
  }
}
