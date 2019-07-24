package com.intuit.superglue.pipeline.parsers

import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.parsers
import org.apache.calcite.sql._
import org.apache.calcite.sql.ddl.{SqlCreateTable, SqlCreateView}
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.sql.parser.ddl.SqlDdlParserImpl
import org.apache.calcite.sql.util.SqlBasicVisitor
import org.apache.calcite.sql.validate.SqlConformanceEnum

import scala.util.{Failure, Success, Try}

class CalciteStatementParser extends StatementParser {
  private val calciteConfig: SqlParser.Config = SqlParser.configBuilder()
    .setParserFactory(SqlDdlParserImpl.FACTORY)
    .setConformance(SqlConformanceEnum.MYSQL_5)
    .build()

  /**
    * Parses the provided string as SQL using the Calcite parser.
    *
    * @return The [[StatementMetadataFragment]] created by analyzing the
    *         contents of the SQL statement.
    */
  override def parseStatement(statement: String): StatementMetadataFragment = {
    val fragment: Try[StatementMetadataFragment] = Try {
      val parser = SqlParser.create(statement, calciteConfig)
      val stmt = parser.parseStmt()
      val visitor = new parsers.CalciteStatementParser.Visitor()
      stmt.accept(visitor)
      visitor.buildMetadata().get
    }

    fragment match {
      case Failure(exception) => StatementMetadataFragment(getClass.getName, List(exception))
      case Success(value) => value
    }
  }
}

object CalciteStatementParser {

  /**
    * Exception type for recording top-level statements which were unexpected.
    */
  case class UnsupportedTopLevelStatementException(node: SqlNode)
    extends Exception(s"${node.getKind.toString}: ${node.getClass.getName}")

  /**
    * Visits each node of the SQL Abstract Syntax Tree. This inspects each node to see
    * whether it contains information regarding input or output tables. After being
    * accepted by an AST, this visitor can be queried for the metadata it gathered by
    * using [[buildMetadata()]].
    */
  class Visitor extends SqlBasicVisitor[Unit] {
    private var statementType: Option[String] = Option.empty[String]
    private var inputTables: Set[String] = Set.empty[String]
    private var outputTables: Set[String] = Set.empty[String]
    private var errors: Set[Throwable] = Set.empty[Throwable]

    /**
      * Returns the metadata that the visitor collected after visiting each node in
      * the AST. This method returns [[None]] if it was called before this visitor
      * performs any traversal, or Some([[StatementMetadataFragment]]) if the visitor
      * has already run.
      */
    def buildMetadata(): Option[StatementMetadataFragment] = statementType.map { stmtType =>
      StatementMetadataFragment(
        getClass.getName,
        stmtType,
        inputTables.toList,
        outputTables.toList,
        errors.toList
      )
    }

    /**
      * Calcite calls this function once for each SQL "call" node in the parsed
      * AST. We detect which call types we want to analyze and call the appropriate
      * handler internally. For example, we analyze each type of statement that
      * might have a table name in it, e.g. "SELECT", "INSERT", "UPDATE", etc.
      */
    override def visit(call: SqlCall): Unit = {

      // Assign the top-level statement type when we visit the first call.
      val topLevel = if (statementType.isDefined) false else {
        statementType = Some(call.getKind.toString)
        true
      }

      // Call the appropriate handler based on the type of the visited node.
      call match {
        case createTable: SqlCreateTable => visitCreateTable(createTable)
        case createView: SqlCreateView => visitCreateView(createView)
        case insert: SqlInsert => visitInsert(insert)
        case select: SqlSelect => visitSelect(select)
        case update: SqlUpdate => visitUpdate(update)
        case merge: SqlMerge => visitMerge(merge)
        // The following cases represent nodes that we know about,
        // but which don't hold any data we care about.
        case except: SqlCall if except.getKind == SqlKind.EXCEPT =>
        case createFunc: SqlCall if createFunc.getKind == SqlKind.CREATE_FUNCTION =>
        case union: SqlBasicCall if union.getKind == SqlKind.UNION =>
        case _: SqlDelete =>
        case _: SqlDrop =>
        case _: SqlWith =>
        case _: SqlOrderBy =>
        case _: SqlSetOption =>
        case call: SqlCall if topLevel => errors += UnsupportedTopLevelStatementException(call)
        case _ =>
      }

      // Parent provides tree traversal logic
      super.visit(call)
    }

    /** Records an identifier as the name of an input table */
    private def visitInputIdentifier(id: SqlIdentifier): Unit = inputTables += id.toString

    /** Records an identifier as the name of an output table */
    private def visitOutputIdentifier(id: SqlIdentifier): Unit = outputTables += id.toString

    /**
      * In a CREATE TABLE statement, we record the name of the created
      * table as an output table.
      */
    private def visitCreateTable(createTable: SqlCreateTable): Unit = {
      // Visit the name of the table being created
      createTable.getOperandList.get(0) match {
        case id: SqlIdentifier => visitOutputIdentifier(id)
        case _ =>
      }
    }

    /**
      * In a CREATE VIEW statement, we record the name of the created
      * view as an output table.
      */
    private def visitCreateView(createView: SqlCreateView): Unit = {
      // Visit the name of the view being created
      createView.getOperandList.get(0) match {
        case id: SqlIdentifier => visitOutputIdentifier(id)
        case _ =>
      }
    }

    /**
      * In an INSERT statement, we record the name of the target table as
      * an output table, and the name of a source table as an input table.
      */
    private def visitInsert(insert: SqlInsert): Unit = {
      // Visit "source" node of insert statement
      insert.getSource match {
        case id: SqlIdentifier => visitInputIdentifier(id)
        case _ =>
      }

      // Visit "target" node of insert statement
      insert.getTargetTable match {
        case id: SqlIdentifier => visitOutputIdentifier(id)
        case _ =>
      }
    }

    /**
      * In a SELECT statement, we record the names of all tables mentioned
      * in the "FROM" clause as input tables. This includes all names of
      * joined tables.
      */
    private def visitSelect(select: SqlSelect): Unit = {
      // Visit the FROM branch of the select statement
      select.getFrom match {
        case id: SqlIdentifier => visitInputIdentifier(id)
        case as: SqlBasicCall if as.getKind == SqlKind.AS => visitAs(as)
        case join: SqlJoin => visitJoin(join)
        case _ =>
      }

      // Helper for visiting a Join statement inside of a Select statement
      def visitJoin(join: SqlJoin): Unit = List(join.getLeft, join.getRight).foreach {
        case id: SqlIdentifier => visitInputIdentifier(id)
        case as: SqlBasicCall if as.getKind == SqlKind.AS => visitAs(as)
        case childJoin: SqlJoin => visitJoin(childJoin)
        case _ =>
      }

      // Visit an AS statement inside a SELECT statement
      def visitAs(basic: SqlBasicCall): Unit = {
        // Each AS statement has two operands. We only care about the first (second is an alias)
        val importantNode = basic.getOperandList.get(0)
        importantNode match {
          case id: SqlIdentifier => visitInputIdentifier(id)
          case _ =>
        }
      }
    }

    /**
      * In an UPDATE statement, we record the name of the target table
      * as an output table.
      */
    private def visitUpdate(update: SqlUpdate): Unit = {
      update.getTargetTable match {
        case id: SqlIdentifier => visitOutputIdentifier(id)
        case _ =>
      }
    }

    /**
      * In a MERGE statement, we record the name of the target table
      * as an output table and the name of the source table as an input table.
      */
    private def visitMerge(merge: SqlMerge): Unit = {
      merge.getTargetTable match {
        case id: SqlIdentifier => visitOutputIdentifier(id)
        case _ =>
      }

      merge.getSourceTableRef match {
        case id: SqlIdentifier => visitInputIdentifier(id)
        case as: SqlBasicCall if as.getKind == SqlKind.AS => visitSourceAs(as)
        case _ =>
      }

      def visitSourceAs(basic: SqlBasicCall): Unit = {
        val importantNode = basic.getOperandList.get(0)
        importantNode match {
          case id: SqlIdentifier => visitInputIdentifier(id)
          case _ =>
        }
      }
    }
  }

}
