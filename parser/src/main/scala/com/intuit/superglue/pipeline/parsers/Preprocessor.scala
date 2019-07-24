package com.intuit.superglue.pipeline.parsers

import java.io.Reader

/**
  * Performs text-editing on the whole-body input of a given script.
  *
  * Preparsers may be composed with one another, so the input to a given
  * preprocessor may not be the raw input of the script, but may already
  * be preprocessed by a previous Preparser.
  *
  * Preparsing is performed on [[Reader]]s in order to allow lazy evaluation
  * and potentially reduce memory footprint. A given implementation of
  * Preparser may choose to buffer the input to a String, or may use
  * stream-editing techniques on the Reader itself.
  */
trait Preprocessor {
  /**
    * Given an input-text Reader, return a Reader that applies text modifications
    * to the stream.
    *
    * @param input The whole-script input Reader.
    * @return A Reader with applied text-editing.
    */
  def preprocess(input: Reader): Reader
}
