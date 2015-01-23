package org.oseraf.bullseye.store

import com.typesafe.scalalogging.slf4j.Logging
import org.oseraf.bullseye.store.impl.blueprints.BlueprintsGraphStore

import scala.util.{Failure, Success, Try}
import com.thinkaurelius.titan.core.attribute.Text._
import scala.collection.JavaConversions._
/**
 * Created by nhamblet.
 */
trait AttributeBasedNaivelyFuzzySearchPlugin {
  // returns scores from 0 to 1
  def search(key: AttributeContainer.KEY, value: AttributeContainer.VALUE): Iterable[(EntityStore.ID, Double)]
}


trait AttributeBasedSearchAvailableOptionsPlugin {
  def searchableAttributes: Iterable[(AttributeContainer.KEY, String)]
}

trait BruteForceAttributeBasedNaivelyFuzzySearchPlugin
  extends AttributeBasedNaivelyFuzzySearchPlugin
{
  val store: EntityStore with EntityIterationPlugin

  override def search(key: AttributeContainer.KEY, value: AttributeContainer.VALUE): Iterable[(EntityStore.ID, Double)] =
    store.entities.filter( entityId => {
      val attrMatch = attr(entityId, store.entity(entityId), key, "")
      Try(attrMatch.matches("(?i).*" + value + ".*")) match {
        case Success(v) => v
        case Failure(e) => Try(attrMatch.matches(value)) match {
          case Success(v) => v
          case Failure(e) => false
        }
      }
    }).map(eid => (eid, score(attr(eid, store.entity(eid), key, ""), value))).toSeq.sortBy(-_._2)

  def score(actual: AttributeContainer.VALUE, expected: AttributeContainer.VALUE): Double = {
    actual match {
      case s: String if s == expected                                 => 1.0
      case s: String if s.equalsIgnoreCase(expected)                  => 0.95
      case s: String if s.matches(".*\\b" + expected + "\\b.*")       => 0.85
      case s: String if s.matches("(?i).*\\b" + expected + "\\b.*")   => 0.80
      case s: String if s.matches(".*\\b" + expected + ".*")          => 0.75
      case s: String if s.matches("(?i).*\\b" + expected + ".*")      => 0.70
      case _                                                          => 0.50
    }
  }

  private def attr(entityId: EntityStore.ID, entity: Entity, key: AttributeContainer.KEY, default: AttributeContainer.VALUE = "") =
    if (key == AttributeBasedSearch.FAKE_ID_ATTRIBUTE_KEY) entityId
    else entity.attribute(key, default)
}

trait IndexedBlueprintsFuzzyVertexSearchPlugin extends AttributeBasedNaivelyFuzzySearchPlugin with BruteForceAttributeBasedNaivelyFuzzySearchPlugin with Logging  {
  val store:BlueprintsGraphStore
  override def search(key: AttributeContainer.KEY, value:AttributeContainer.VALUE):Iterable[(EntityStore.ID, Double)] = {
    logger.info("using indexed search")
    store.graph.query().has(key, CONTAINS, value).vertices().toList.map(v => (v.getId.toString, score(v.getProperty(key), value)))
  }
}

object AttributeBasedSearch {
  final val FAKE_ID_ATTRIBUTE_KEY = "OSERAF:search/id"
}