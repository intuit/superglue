package com.intuit.superglue.pipeline.producers

import java.io.InputStream

import scala.util.Try

/**
  * Inputs to the parser have a name, a kind, and a way to read
  * from an InputStream.
  *
  * The name is just for readability, but the kind is used to allow
  * parser implementations to filter for only the inputs they support.
  */
trait ScriptInput {
  def name: String
  def kind: String
  def dialect: Option[String]
  def source: String

  /**
    * Sends the [[InputStream]] of this Input to the given function.
    * This function returns the same value that the given function returns.
    *
    * @param f A function that takes the [[InputStream]].
    * @tparam R The type of the value returned by the given function.
    * @return The same value received from executing the given function.
    */
  def readInputStream[R](f: InputStream => R): Try[R]
}
