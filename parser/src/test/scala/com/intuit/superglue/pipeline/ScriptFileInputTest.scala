package com.intuit.superglue.pipeline

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.stream.Collectors

import com.intuit.superglue.pipeline.producers.ScriptFileInput

class ScriptFileInputTest extends FsSpec {

  "A ScriptFileInput" should "open and read text from a file" in {
    val f = Fixture(List("/test/file.txt"))
    val testFile = f.root.resolve("/test/file.txt")
    Files.write(testFile, "The quick brown fox jumped over the lazy dog".getBytes(Charset.defaultCharset()))

    val input = ScriptFileInput(testFile, "test.txt", "TXT", None)
    val testContents = input.readInputStream { inputStream =>
      new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"))
    }
    assert(input.source.equals("FILE"))
    assert(testContents.isSuccess)
    assert(testContents.get.equals("The quick brown fox jumped over the lazy dog"))
  }
}
