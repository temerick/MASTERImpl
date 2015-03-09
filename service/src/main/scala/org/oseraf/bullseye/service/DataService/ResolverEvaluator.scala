package org.oseraf.bullseye.service.DataService

/**
 * Created by sstyer on 3/9/15.
 */

import org.oseraf.bullseye.store.EntityStore

import scala.collection.mutable

trait ResolverEvaluator {
  val resolver:Resolver

  def toQuintInt(dub:Double):Int = (((dub * 20).toInt / 20.0) * 100).toInt

  def numberDupsVsThreshold(resolver:DukeResolver, start:Int=65, step:Int=5, end:Int=100):Map[Int, Int] = {
    var threshMap = new mutable.HashMap[Int, Int]
    var ents = new mutable.HashSet[EntityStore.ID]
    for (i <- start to end by step) threshMap.put(i, 0)

    resolver.deduplicate(0)
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
    resolver.deduplicate()
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
}
