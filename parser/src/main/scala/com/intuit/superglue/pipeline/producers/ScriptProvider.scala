package com.intuit.superglue.pipeline.producers

/**
  * The type of objects which can read scripts from various sources
  * in order to be parsed. See [[ScriptFileProvider]] as an example
  * implementation.
  */
trait ScriptProvider {
  def stream(): Iterator[ScriptInput]
}
