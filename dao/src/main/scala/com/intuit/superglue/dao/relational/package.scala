package com.intuit.superglue.dao

package object relational {

  /**
    * A trait for types which have a JDBC profile.
    *
    * Given a type which implements [[Profile]], the DAO traits
    * in this package can be "mixed in" on that type in order to
    * provide implementations of the traits' queries while using
    * the correct JDBC profile for generating SQL. This allows the
    * mixin traits to implement their queries in a way that's
    * decoupled from the JDBC profile or SQL dialect that's required.
    *
    * See [[SuperglueRelationalRepository.DataLayer]] and its
    * instance, [[SuperglueRelationalRepository.dataLayer]].
    */
  trait Profile {
    val profile: slick.jdbc.JdbcProfile
  }
}
