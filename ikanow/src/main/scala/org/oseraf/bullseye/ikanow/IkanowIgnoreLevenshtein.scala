package org.oseraf.bullseye.ikanow

import no.priv.garshol.duke.Comparator
import no.priv.garshol.duke.comparators.Levenshtein

class IkanowIgnoreLevenshtein() extends Comparator {
  val lev = new Levenshtein
  private var ignores:String = ""

  def isTokenized:Boolean = {
    false
  }
  def setIgnores(igs:String) = {
    ignores = igs
  }
  def compare(v1:String, v2:String):Double = {
    val ignoredStrs = ignores.split(",")
    if(ignoredStrs.contains(v1) || ignoredStrs.contains(v2)) {
      0.0
    }
    else {
      lev.compare(v1,v2)
    }
  }
}
