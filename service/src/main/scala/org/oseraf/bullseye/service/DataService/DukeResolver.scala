package org.oseraf.bullseye.service.DataService

import java.util

import com.typesafe.scalalogging.slf4j.Logging
import no.priv.garshol.duke._
import org.oseraf.bullseye.service.Service
import org.oseraf.bullseye.store._

import scala.collection.JavaConversions._
import scala.util.Try


class DukeResolver(
                    store: EntityStore with EntityIterationPlugin with WriteEventPublisherPlugin,
                    dukeConf: Configuration,
                    reindex: Boolean
                    )
  extends Service with Logging with WriteEventListener
{

  val keeperProps = dukeConf.getProperties.map(_.getName)
  val innerDB = {
    val db = dukeConf.getDatabase(reindex)
    if (reindex) {
      logger.info("Indexing graph in Duke, properties: " + keeperProps.mkString(", "))
      var indexCount = 0
      store.entities.foreach(eid => {
        db.index(new EntityRecord(eid, store.entity(eid)))
        indexCount += 1
        if (indexCount % 1000 == 0) {
          logger.info(s"Indexed $indexCount, inMemory: ${db.isInMemory}")
        }
      })
      db.commit()
    } else {
      Try(db.index(null)) // force init for LuceneDatabase
      logger.info("Using saved index: " + db.toString)
    }
    db
  }

  // how do we handle updates and deletions?
  store.addListener(this)
  override def handleAddEntityEvent(id: EntityStore.ID) = {
    logger.debug("Detected add entity, updating resolver index")
    innerDB.index(new EntityRecord(id, store.entity(id)))
    innerDB.commit()
  }
  override def handleAddRelationshipEvent(id: EntityStore.ID) = {}
  override def handleUpdateEntityEvent(id: EntityStore.ID) = {}
  override def handleUpdateRelationshipEvent(id: EntityStore.ID) = {}
  override def handleRemoveEntityEvent(id: EntityStore.ID) = {}
  override def handleRemoveRelationshipEvent(id: EntityStore.ID) = {}

  // we only really want this for the compare method
  // we end up rolling our own processing (hence the innerDB above), similar to some Processor flow
  //   (link from dataset of size 1, with simple comparison))
  val dukeProcessor = new Processor(dukeConf)

  def deduplicate(): Seq[BullsEyeDedupeCandidate] = {
    store
      .entities
      .flatMap(entityId => {
        resolve(entityId)
          .map(dupe =>
            BullsEyeDedupeCandidate(
              Seq(toBullsEyeEntity(new EntityRecord(entityId, store.entity(entityId))), dupe.entity).toSet,
              dupe.score
            )
          )
    }).toSet.toSeq
  }

  def resolve(targetEntityId: String, limit: Option[Int] = None): Seq[BullsEyeEntityScore] = {
    val targetRecord = new EntityRecord(targetEntityId, store.entity(targetEntityId))
    val candidates = innerDB.findCandidateMatches(targetRecord)
    logger.debug("Resolving targetRecord " + targetEntityId + " among " + candidates.size() + " candidates")
    candidates.flatMap(candidate => compare(targetRecord, candidate)).toSeq
  }

  private def compare(targetRecord: EntityRecord, candidateRecord: Record): Option[BullsEyeEntityScore] = {
    if (areEquivalent(targetRecord, candidateRecord)) {
      None
    } else {
      val score = dukeProcessor.compare(targetRecord, candidateRecord)
      logger.trace("Got comparison score " + score + " for " + candidateRecord.getValue("id"))
      if (score >= dukeConf.getThreshold) {
        Some(BullsEyeEntityScore(toBullsEyeEntity(candidateRecord), score))
      } else {
        None
      }
    }
  }

  // Processor.isSameAs, but that's private
  private def areEquivalent(left: Record, right: Record): Boolean =
    dukeConf
      .getIdentityProperties
      .exists {
        case prop: Property => left.getValues(prop.getName).toSet.intersect(right.getValues(prop.getName).toSet).nonEmpty
      }

  private def toBullsEyeEntity(record: Record): BullsEyeEntity = {
    val entityId = record.getValue(DukeResolver.ID_ATTRIBUTE)
    new EntityRecord(entityId, store.entity(entityId)).toBullsEyeEntity
  }


  class EntityRecord(val entityId: EntityStore.ID, val entity: Entity) extends Record {
    override def getProperties(): util.Collection[String] =
      entity.attributes.keys.filter(prop => keeperProps.contains(prop)) ++ Seq(DukeResolver.ID_ATTRIBUTE)

    override def getValues(prop: String): util.Collection[String] = {
      val base = Seq(safeProperty(prop))
      prop match {
        case p: String if p == DukeResolver.ID_ATTRIBUTE => base ++ Seq(entityId)
        case _                                           => base
      }
    }

    override def getValue(prop: String): String = {
      prop match {
        case p: String if p == DukeResolver.ID_ATTRIBUTE => entityId
        case _                                           => safeProperty(prop)
      }
    }

    override def merge(other: Record) = ???

    def toBullsEyeEntity: BullsEyeEntity =
      UiConverter.EntityToBullsEyeEntity(entityId, entity)

    private def safeProperty(prop: String): String = {
      entity.attribute(prop, null) match {
        case s: String if s != null => s
        case _                      => ""
      }
    }
  }
}


object DukeResolver {
  final val ID_ATTRIBUTE = "OSERAF:resolve/duke/id"
}

class GaussianNumericComparator extends Comparator {
  private var sigma: Double = 1.0
  private var sigmaSq = 1.0
  private var powerCoef = -0.5
  private final val sqrtTwoPi = 2.50662827
  private var linearCoef = 1.0 / sqrtTwoPi

  def isTokenized: Boolean = {
    false
  }

  def setSigma(s: Double) {
    sigma = s
    updateConstants()
  }

  private def updateConstants() {
    sigmaSq = sigma * sigma
    powerCoef = -1.0 / (2.0 * sigmaSq)
    linearCoef = 1.0 / (sigma * sqrtTwoPi)
  }

  def compare(v1: String, v2: String): Double = {
    var d1: Double = 0.0
    var d2: Double = 0.0
    try {
      d1 = v1.toDouble
      d2 = v2.toDouble
    }
    catch {
      case e: NumberFormatException => return 0.5
    }
    val diff = Math.abs(d1 - d2)
    Math.exp(powerCoef * diff * diff)
  }
}