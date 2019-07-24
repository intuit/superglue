package com.intuit.superglue.pipeline.producers

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.{Files, Path}

import io.tmos.arm.ArmMethods.manage

import scala.util.Try

/**
  * An input to the parser given by a file on disk.
  *
  * @param path The filepath where to read the file from.
  * @param name The filename (stringified path) of the input.
  * @param kind The type of this input. Used to determine which
  *                  parser will process this input.
  */
case class ScriptFileInput(
  path: Path,
  name: String,
  kind: String
) extends ScriptInput {
  override def source: String = "FILE"
  override def readInputStream[R](f: InputStream => R): Try[R] = Try {
    for {
      fileInputStream <- manage(Files.newInputStream(path))
      bufferedInputStream <- manage(new BufferedInputStream(fileInputStream))
    } yield {
      f(bufferedInputStream)
    }
  }
}
