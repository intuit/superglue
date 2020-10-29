package com.intuit.superglue.pipeline.parsers

import java.io.{BufferedReader, InputStreamReader, Reader}
import java.time.LocalDateTime

import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.producers.ScriptInput
import com.typesafe.config.{Config => TypesafeConfig}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * A StagedParser parses inputs in three stages: Preparsing, Splitting, and Parsing.
  *
  * Clients may directly instantiate StagedParser by passing the required components in
  * the constructor, or they may use a subclass that provides preconfigured components.
  * See [[SqlScriptParser]].
  *
  * =Stages=
  * ==Preparsing==
  *
  * Preparsing is a raw text-editing stage in which preparsers are provided with a [[Reader]]
  * that supplies the entire contents of the input script directly from the [[ScriptInput]] object,
  * and are expected to return another Reader with any desired modifications to the stream
  * (e.g. in-stream regex find-and-replace).
  * Note that preparsers may be composed with other preparsers, so any given preparser is not
  * guaranteed to have the "raw" input, but rather a version of the input that has already
  * passed through a preparser.
  *
  * ==Splitting==
  *
  * Splitting is where we take the fully-preprocessed script and break it into its individual
  * statements. See [[SimpleStatementSplitter]] for an example implementation.
  *
  * ==Statement Parsing==
  *
  * The Statement Parsing step is where we extract useful metadata from individual statements, either
  * through custom text analysis or by leveraging another parsing library. [[StatementParser]]s take a
  * statement as a String input, and they produce a [[StatementMetadata]]. A Metadata is a case class
  * containing fields that we want to fill with information we've found out about the statement.
  *
  * Metadata is given in two parts. The first part is data which may be gleaned regardless of
  * whether the parser implementation fails to parse. Examples include the type of statement that
  * we're trying to parse. The second part is "fallible" data, which will not be returned in the case
  * that a parser fails and throws an exception. See [[StatementMetadata]].
  *
  * @param preparsers      A collection of preparsers to pass the script through. The preparsers will
  *                        be applied in the order they are retrieved from the Collection.
  * @param splitter        A function which takes a whole script and breaks it into individual statements.
  * @param statementParser A parser which analyzes individual statements to extract metadata.
  */
abstract class StagedScriptParser(
  protected val preparsers: List[Preprocessor],
  protected val splitter: StatementSplitter,
  protected val statementParser: StatementParser,
)(implicit rootConfig: TypesafeConfig) extends ScriptParser {
  private val scriptConfig = rootConfig.getConfig(s"com.intuit.superglue.pipeline.parsers.$parserName")
  private val inputKinds = scriptConfig.getStringList("input-kinds").asScala

  /**
    * This parser accepts any input kinds which were included in the configuration.
    */
  override def acceptsKind(kind: String): Boolean = inputKinds.contains(kind)

  override def parse(input: ScriptInput): Future[ScriptMetadata] = Future {
    val scriptParseStartTime = LocalDateTime.now()

    /**
      * When parsing a script body fails, we create an output object that contains
      * the exception that was thrown.
      */
    def scriptPreprocessingFailed(thrown: Throwable): ScriptMetadata = ScriptMetadata(
      input.name,
      input.source,
      input.kind,
      input.dialect,
      parserName,
      scriptParseStartTime,
      LocalDateTime.now(),
      List.empty[StatementMetadata],
      List(thrown),
    )

    /**
      * Given a list of Try[R], return a list of successful and of failed values
      */
    def collectSuccessAndErrors[R](list: List[Try[R]]): (List[R], List[Throwable]) = {
      var (elements, errors) = (List.empty[R], List.empty[Throwable])
      list.foreach {
        case Failure(thrown) => errors :+= thrown
        case Success(value) => elements :+= value
      }
      (elements, errors)
    }

    /**
      * If the preparsing step completes successfully, we continue the parsing process.
      * Here, we split up the individual statements of the script and parse them
      * individually with the provided statementParser. The metadata of all the statements
      * is collected into a "script" metadata object.
      */
    def scriptPreprocessingSuccessful(body: String): ScriptMetadata = {
      // Split the (preprocessed) body of the script into it's individual statements
      val statements = splitter.splitStatements(body).asScala.zipWithIndex

      // Each statement is passed through all of the analyzers
      val statementMetadataTries = statements.map { case (statement, statementIndex) => Try {

        val statementParseStartTime = LocalDateTime.now()

        // Parse the statement and get the statement metadata back.
        val metadataFragment = statementParser.parseStatement(statement, input.dialect)

        // Metadata collected about this one statement
        StatementMetadata(
          statement,
          statementIndex,
          statementParseStartTime,
          LocalDateTime.now(),
          metadataFragment,
        )
      }}.toList

      val (statementMetadata, statementErrors) = collectSuccessAndErrors(statementMetadataTries)

      // Metadata collected about this whole script
      ScriptMetadata(
        input.name,
        input.source,
        input.kind,
        input.dialect,
        parserName,
        scriptParseStartTime,
        LocalDateTime.now(),
        statementMetadata,
        statementErrors,
      )
    }

    // Read the script from the input stream and perform pre-processing
    val scriptBody = input.readInputStream { inputStream =>
      val inputReader = new InputStreamReader(inputStream)

      // Compose all of the preparsers one after the other
      var decoratedReader: Reader = inputReader
      preparsers.foreach(preparser => decoratedReader = preparser.preprocess(decoratedReader))

      // Collect the processed script as a String
      val buffered = new BufferedReader(decoratedReader)
      Stream.continually(buffered.readLine()).takeWhile(_ != null).mkString("\n")
    }

    scriptBody match {
      case Failure(thrown) => scriptPreprocessingFailed(thrown)
      case Success(body) => scriptPreprocessingSuccessful(body)
    }

  }
}
