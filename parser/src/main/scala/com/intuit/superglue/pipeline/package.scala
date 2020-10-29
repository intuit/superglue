package com.intuit.superglue

import com.typesafe.config.{Config => TypesafeConfig}
import pureconfig.generic.auto._

package object pipeline {
  case class FileInputConfig(
    base: String,
    kind: Option[String] = None,
    dialect: Option[String] = None,
    includes: List[String] = List.empty,
    excludes: List[String] = List.empty,
  )

  object FileInputConfig {
    def apply(config: TypesafeConfig): Option[FileInputConfig] =
      pureconfig.loadConfig[FileInputConfig](config).toOption
  }

  object Implicits {
    implicit class StringEtc(s: String) {
      /**
        * A postfix operator for trimming a string to a given length
        * and adding ellipses if the string was too long.
        *
        * {{{
        *   import Implicits.StringEtc
        *   val string = "INFO: Some really long status that we only need the first few words from"
        *   val printString = string etc 30
        *   assertEquals(printString, "INFO: Some really long stat...")
        * }}}
        *
        * @param i The length of the trimmed string to be output.
        */
      def etc(i: Int): String = {
        if (s.length <= i) s else s.substring(0, i-3) + "..."
      }
    }
  }
}
