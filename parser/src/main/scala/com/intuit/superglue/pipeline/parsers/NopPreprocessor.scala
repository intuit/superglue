package com.intuit.superglue.pipeline.parsers

import java.io.Reader

/**
  * An implementation of Preparser that does not edit the Reader stream.
  */
object NopPreprocessor extends Preprocessor {
  override def preprocess(input: Reader): Reader = input
}
