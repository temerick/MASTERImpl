package org.oseraf.bullseye.ikanow

import no.priv.garshol.duke.Comparator
import no.priv.garshol.duke.comparators.Levenshtein

class IkanowTypeComparator extends Comparator {
  def isTokenized:Boolean = {
    false
  }

  def compare(v1:String, v2:String):Double = {
    if(v1 == "Date" || v2 == "Date") {
      0.0
    }
    else {
      val l = new Levenshtein()
      l.compare(v1,v2)
    }
  }
}
