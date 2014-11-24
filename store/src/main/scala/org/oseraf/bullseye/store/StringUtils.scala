package org.oseraf.bullseye.store

/**
 * Created by nhamblet.
 */
object StringUtils {

  def toDisplayName(str: String): String =
    // camel-case splitter from
    //    http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
    str.replaceAll(
      String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"),
      " "
    ).split(Array(' ', '_')).map(_.capitalize).mkString(" ")

}
