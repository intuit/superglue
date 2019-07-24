package com.intuit.superglue.cli

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.file.{FileSystem, Files, Path}

import com.google.common.jimfs.{Configuration, Jimfs}
import org.scalatest.FlatSpec

trait CliSpec extends FlatSpec {

  class Fixture {
    val fs: FileSystem = Jimfs.newFileSystem(Configuration.unix())
    val root: Path = fs.getPath("/")
    Files.createDirectories(root)
    val stdoutBuffer = new ByteArrayOutputStream()
    val stdout = new PrintStream(stdoutBuffer)
    def stdoutString: String = stdoutBuffer.toString
    val main: Main = new Main(stdout, fs)
  }

  object Fixture {
    def apply(f: Fixture => Unit): Unit = f(new Fixture())
  }
}
