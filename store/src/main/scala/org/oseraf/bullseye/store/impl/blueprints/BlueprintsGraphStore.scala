package org.oseraf.bullseye.store.impl.blueprints

import com.thinkaurelius.titan.core.TitanGraph
import com.tinkerpop.blueprints._
import org.oseraf.bullseye.store._

import scala.collection.JavaConversions._

/**
 * Created by nhamblet.
 */
trait BlueprintsPlugin {
  val graph: Graph
}

trait BlueprintsVertexIteratorPlugin
  extends BlueprintsPlugin
     with EntityIterationPlugin {
  override def entities: Iterable[EntityStore.ID] =
    graph.getVertices.map(v => v.getId.toString)
}

trait BlueprintsEdgeIteratorPlugin
  extends BlueprintsPlugin
     with RelationshipIterationPlugin {
  override def relationships: Iterable[EntityStore.ID] =
    graph.getEdges.map(e => e.getId.toString)
}

trait BlueprintsNeighborhoodPlugin
  extends BlueprintsPlugin
     with NeighborhoodPlugin {
  override def neighborhood(entityId: EntityStore.ID) =
    graph.getVertex(entityId).getEdges(Direction.BOTH).map(e => e.getId.toString)
}


class BlueprintsGraphStore(val graph: Graph)
  extends EntityStore
    with AttributeBasedSearchAvailableOptionsPlugin
    with BlueprintsNeighborhoodPlugin
    with BlueprintsVertexIteratorPlugin
    with BlueprintsEdgeIteratorPlugin
    with SplicePlugin
    with BruteForceAttributeBasedNaivelyFuzzySearchPlugin
{
  override val store = this

  override def entity(id: EntityStore.ID) =
    new BlueprintsEntity(graph.getVertex(id))

  override def relationship(id: EntityStore.ID) =
    new BlueprintsRelationship(graph.getEdge(id))

  override def addEntity(id: EntityStore.ID, entity: Entity) = {
    val vertex = graph.addVertex(id)
    copyAttributes(entity, vertex)
    true
  }

  override def addRelationship(id: EntityStore.ID, relationship: Relationship) = {
    relationship.connecting().toList match {
      case List(sourceId, targetId) =>
        val source = graph.getVertex(sourceId)
        val target = graph.getVertex(targetId)
        val label = relationship.attribute(BlueprintsGraphStore.RELATIONSHIP_LABEL_SOURCE_ATTRIBUTE, BlueprintsGraphStore.DEFAULT_EDGE_LABEL)
        val edge = graph.addEdge(id, source, target, label)
        copyAttributes(relationship, edge)
        true
      case _ =>
        false
    }
  }

  override def entityExists(id: EntityStore.ID) =
    graph.getVertex(id) != null

  override def relationshipExists(id: EntityStore.ID) =
    graph.getEdge(id) != null

  override def removeEntity(id: EntityStore.ID) = {
    graph.removeVertex(graph.getVertex(id))
    true
  }

  override def removeRelationship(id: EntityStore.ID) = {
    graph.removeEdge(graph.getEdge(id))
    true
  }

  override def updateEntity(id: EntityStore.ID, entity: Entity) = {
    val vertex = graph.getVertex(id)
    vertex.getPropertyKeys.foreach(key => vertex.removeProperty[String](key))
    copyAttributes(entity, vertex)
    true
  }

  override def updateRelationship(id: EntityStore.ID, relationship: Relationship) = {
    val edge = graph.getEdge(id)
    edge.getPropertyKeys.foreach(key => edge.removeProperty[String](key))
    copyAttributes(relationship, edge)
    true
  }

  lazy val searchableAttributes: Iterable[(AttributeContainer.KEY, String)] =
    graph.getVertices
      .foldLeft(Set[String]()) { case (attrs, vertex) => attrs ++ vertex.getPropertyKeys }
      .map(key => (key, StringUtils.toDisplayName(key)))
      .toSeq ++ Seq((AttributeBasedSearch.FAKE_ID_ATTRIBUTE_KEY, "ID"))

  private def copyAttributes(container: AttributeContainer, element: Element) =
    container.attributes.foreach(p => element.setProperty(p._1, p._2))
}

object BlueprintsGraphStore {
  final val DEFAULT_EDGE_LABEL = ""
  final val RELATIONSHIP_LABEL_SOURCE_ATTRIBUTE = "OSERAF:store/blueprints/edge/label"

  def apply(graph: Graph) = {
    new BlueprintsGraphStore(graph)
  }
}


class BlueprintsAttributeContainer(el: Element)
  extends AttributeContainer
{
  override def attribute(key: AttributeContainer.KEY) =
    el.getProperty[AttributeContainer.VALUE](key)

  override var attributes =
    el.getPropertyKeys.map(k => k -> el.getProperty[AttributeContainer.VALUE](k)).toMap
}


class BlueprintsEntity(vertex: Vertex)
  extends BlueprintsAttributeContainer(vertex)
     with Entity


class BlueprintsRelationship(edge: Edge)
  extends BlueprintsAttributeContainer(edge)
     with BinaryRelationship {

  override def from =
    edge.getVertex(Direction.OUT).getId.toString

  override def to =
    edge.getVertex(Direction.IN).getId.toString

  override def isDirected =
    true
}

class IndexedBlueprintsGraphStore(g:Graph) extends BlueprintsGraphStore(g) with IndexedBlueprintsFuzzyVertexSearchPlugin
