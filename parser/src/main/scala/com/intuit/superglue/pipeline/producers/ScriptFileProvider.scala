package com.intuit.superglue.pipeline.producers

import java.nio.file._

import com.intuit.superglue.pipeline._
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

/**
  * A FileProvider takes in a config describing one or more files to read,
  * and generates a stream of Input objects which can open the InputStreams
  * of the described files.
  *
  * These config parameters are described in the
  * [[https://github.intuit.com/pages/quickdata/superglue/parser-configuration.html#files files]]
  * section of the parser configuration page.
  *
  * @param config The configuration object that drives the behavior of the FileProvider.
  */
case class ScriptFileProvider(private val config: FileInputConfig)(implicit fs: FileSystem) extends ScriptProvider {
  private val logger = Logger[this.type]
  private val basePath = fs.getPath(config.base)
  private val includes = config.includes.map(ScriptFileProvider.getPathMatcher(basePath))
  private val excludes = config.excludes.map(ScriptFileProvider.getPathMatcher(basePath))

  override def stream(): Iterator[ScriptInput] = {
    Files.walk(basePath).iterator().asScala
      .filter(!Files.isDirectory(_))

      // If there are no includes patterns, include all files
      // If there are includes patterns, filter out files that don't match them
      .filter(file => includes.isEmpty || includes.exists(_.matches(file)))

      // Filter out any files that match an "exclude" pattern
      .filter(file => !excludes.exists(_.matches(file)))

      // Perform "kind inference" on inputs, and filter out any inputs that we couldn't infer a kind for.
      .map(path => (path, ScriptFileProvider.inferInputKind(path, config.kind)))
      .filter { case (path: Path, kind: Option[_]) =>
        val keep = kind.isDefined
        if (!keep) logger.warn(s"Excluding file with no kind: ${path.toString}")
        keep
      }

      // Create a FileInput for each path that has a kind
      .map { case (path: Path, kind: Option[String]) =>
        ScriptFileInput(path, basePath.relativize(path).toString, kind.get, config.dialect)
      }
  }
}

object ScriptFileProvider {
  private val logger = Logger(getClass)
  private val pattern = "^(glob|regex)?:(.*)".r

  /**
    * The PathMatcher provided by the nio API only supports path strings that start
    * with "glob:" or "regex:". This extends that to give a "literal" path match.
    * If there is no path-match prefix ("glob:" or "regex:") then this will return a
    * matcher that checks if a given path exactly matches the string given.
    * @param string The path-match string. Can be "glob:", "regex:", or literal (no prefix).
    * @return A PathMatcher that matches globs, regexes, and literal paths.
    */
  private def getPathMatcher(prefix: Path)(string: String)(implicit fs: FileSystem): PathMatcher = {
    if (!string.startsWith("glob:") && string.contains("*")) {
      logger.warn(s"""File include pattern looks like a glob, did you mean "glob:${string.replace("\n", " ")}"?""")
    }

    string match {
      // Glob with absolute path
      case pattern("glob", p) if p.startsWith("/") => fs.getPathMatcher(string)
      // Regex with absolute path
      case pattern("regex", p) if p.startsWith("/") => fs.getPathMatcher(string)
      // Glob with relative path gets resolved against the prefix
      case pattern("glob", p) => fs.getPathMatcher(s"glob:${prefix.resolve(p).toString}")
      // Regex with relative path gets resolved against the prefix
      case pattern("regex", p) => fs.getPathMatcher(s"regex:${prefix.resolve(p).toString}")
      case _ => (visitedPath: Path) => {
        // Resolve relative paths against the prefix
        val resolvedPath: Path = if (string.startsWith("/")) fs.getPath(string) else prefix.resolve(string)
        if (resolvedPath == visitedPath) true
        else if (resolvedPath.normalize == visitedPath) true
        else false
      }
    }
  }

  /**
    * Input kinds are strings used to tell each parser which inputs belong to it.
    *
    * Different parser implementations may only support a specific dialect of SQL,
    * therefore each input "group" can be annotated with a kind which the parsers
    * can later filter on.
    *
    * If no input_kind field is provided by the configuration, then here we'll try
    * to infer the type based on the file extension.
    *
    * @param path The [[Path]] of the file we're looking at.
    * @param configuredKind The input_kind from the configuration, if any.
    * @param fs The filesystem (real or test) that we're working with.
    * @return Some(input_kind) if we can infer the input kind, None otherwise.
    */
  private def inferInputKind(path: Path, configuredKind: Option[String])(implicit fs: FileSystem): Option[String] = {
    if (configuredKind.isDefined) {
      return configuredKind
    }

    // Try to infer the kind based on file extension
    if (fs.getPathMatcher("glob:**/*.sql").matches(path)) return Some("sql")
    if (fs.getPathMatcher("glob:**/*.hql").matches(path)) return Some("sql_hive")
    None
  }
}
