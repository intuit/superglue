package com.intuit.superglue.dao

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

package object model {

  /**
    * A Direction indicates the flow of data between two nodes in
    * a Lineage graph. We always interpret a Direction from the
    * perspective of a Script. For example, given a Script and a Table:
    *
    *   An Input indicates that data flows from the Table to the Script, and
    *   an Output indicates that data flows from the Script to the Table.
    */
  sealed trait Direction {
    val name: String
  }
  case object Input extends Direction {
    override val name: String = "Input"
  }
  case object Output extends Direction {
    override val name: String = "Output"
  }

  object Direction {
    def input: Direction = Input
    def output: Direction = Output

    implicit def directionType(implicit profile: slick.jdbc.JdbcProfile): JdbcType[Direction] with BaseTypedType[Direction] = {
      import profile.api._
      MappedColumnType.base[Direction, Char](
        { // Convert Direction to Char
          case Input => 'I'
          case Output => 'O'
        },
        { // Convert Char to Direction
          case 'I' => Input
          case 'O' => Output
        }
      )
    }
  }

}
