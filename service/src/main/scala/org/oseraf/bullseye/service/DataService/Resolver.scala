package org.oseraf.bullseye.service.DataService

import org.oseraf.bullseye.store.{EntityIterationPlugin, EntityStore}

trait Resolver {
  type S

  def resolve(entId:EntityStore.ID):Seq[(EntityStore.ID, S)]

  val store:EntityStore with EntityIterationPlugin

  def resolutions():Map[EntityStore.ID, Seq[(EntityStore.ID, S)]] =
    store.entities.map(tarEnt =>
      tarEnt -> resolve(tarEnt)).toMap

  def deduplicate():Seq[(EntityStore.ID, EntityStore.ID, S)] =
    resolutions()
      .flatMap{
      case(tarEnt, dupCandidates) =>
        dupCandidates.map(dup =>
          (tarEnt, dup._1, dup._2))}.toSeq
}
