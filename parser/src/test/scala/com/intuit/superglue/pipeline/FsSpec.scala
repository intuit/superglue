package com.intuit.superglue.pipeline

import java.nio.file.{FileSystem, Files, Path}

import com.google.common.jimfs.{Configuration, Jimfs}
import org.scalatest.FlatSpec

/**
  * Filesystem Spec, the supertype of tests which need to mock
  * a virtual filesystem.
  */
trait FsSpec extends FlatSpec {

  /**
    * Instantiates a virtual filesystem with the given files.
    * @param filenames A list of files to mock in the virtual fs.
    */
  protected case class Fixture(filenames: Seq[String]) {
    // Instantiate a new virtual filesystem
    val fs: FileSystem = Jimfs.newFileSystem(Configuration.unix())
    // Configure an arbitrary base directory for test
    val root: Path = fs.getPath("/")
    // Create the test directory in the virtual fs
    Files.createDirectories(root)

    // List the files for the test
    val paths: Seq[Path] = filenames.map(root.resolve)
    // Create the files in the virtual fs
    paths.foreach { path =>
      Files.createDirectories(path.getParent)
      Files.createFile(path)
    }
  }

  object Fixture {
    def apply(files: String*)(f: FileSystem => Unit): Unit = {
      val fixture = new Fixture(files)
      f(fixture.fs)
    }
  }
}
