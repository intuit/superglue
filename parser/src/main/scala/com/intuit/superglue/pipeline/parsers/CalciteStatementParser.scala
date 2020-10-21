package com.intuit.superglue.pipeline.parsers

import java.util

import com.intuit.superglue.pipeline.Metadata._
import com.intuit.superglue.pipeline.parsers
import org.apache.calcite.sql._
import org.apache.calcite.sql.ddl.{SqlColumnDeclaration, SqlCreateTable, SqlCreateView}
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.sql.parser.ddl.SqlDdlParserImpl
import org.apache.calcite.sql.util.SqlBasicVisitor
import org.apache.calcite.sql.validate.SqlConformanceEnum
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class CalciteStatementParser extends StatementParser {
  private val calciteConfig: SqlParser.Config = SqlParser.configBuilder()
    .setParserFactory(SqlDdlParserImpl.FACTORY)
    .setConformance(SqlConformanceEnum.BABEL)
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
      visitor.buildMetadata(statement).get
    }

    fragment match {
      case Failure(exception) => {
        println(s"Parsing statement failed ${statement}")
        StatementMetadataFragment(getClass.getName, List(exception))
      }
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
    private var aliasTableMapping:Map[String,String] = Map.empty[String,String]

    private var allAlias:Set[String] = Set.empty[String]

    private var lastVisitedInputTables = StringUtils.EMPTY
    private var tableColumnMapping :Map[String,Set[String]] = Map.empty[String,Set[String]]

    private var aliasMapping :Map[String,SqlSelect] = Map.empty[String,SqlSelect]

    /**
      * Returns the metadata that the visitor collected after visiting each node in
      * the AST. This method returns [[None]] if it was called before this visitor
      * performs any traversal, or Some([[StatementMetadataFragment]]) if the visitor
      * has already run.
      */
    def buildMetadata(statement: String): Option[StatementMetadataFragment] = statementType.map { stmtType =>

      println("****")
      println(statement)
      println(s"inputTables=${inputTables.mkString(",")}")
      println(s"outputTables=${outputTables.mkString(",")}")
      println(s"allAlias = ${allAlias}")
      println(s"columntableMapping= ${tableColumnMapping}")
      println(s"aliasMapping= ${aliasMapping.toString()}")
      println("****")
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

      try {

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
          case orderBy: SqlOrderBy => {
            orderBy.query match {
              case sqlSelect:SqlSelect => visitSelect(sqlSelect)
              case _ =>
            }

            orderBy.orderList match {
              case nodeList:SqlNodeList =>
              case _ =>

            }

          }
          case _: SqlSetOption =>
          case call: SqlCall if topLevel => errors += UnsupportedTopLevelStatementException(call)
          case _ =>
        }

        // cases - aggregates etc .. where column Identifier appears, which are not handled by above calls
        call.getOperandList.asScala.map(node =>{
         node match {
           case as: SqlBasicCall if as.getKind == SqlKind.AS => visitAs(as)
           case sqlIdentifier: SqlIdentifier=>{
             visitSqlIdentifier(sqlIdentifier,lastVisitedInputTables)
           }
           case _ =>
         }
        })

        // Parent provides tree traversal logic
        super.visit(call)
      } catch {
        case ex:Exception => ex.printStackTrace()
      }
    }

    /**
      * Determines mapping of tableName and columns
      * if the identifier contains "." , extracts the alias and identifier name.
      * Get the corresponding table name for the alias
      * @param identifier
      * @param tableName
      */
    private def visitSqlIdentifier(identifier: SqlIdentifier,tableName:String):Unit ={
      // existing columns for tablename
      val existingElements:Option[Set[String]] =  tableColumnMapping.get(tableName)

      if(identifier.names.size() < 2){
        // ignore table names and alias
        if(inputTables.contains(identifier.names.asScala(0)) || outputTables.contains(identifier.names.asScala(0)) ||
          allAlias.contains(identifier.names.asScala(0))){
          // do nothing
        }
        else if (existingElements.isDefined){
          tableColumnMapping += (tableName -> (existingElements.get + identifier.names.asScala(0)))
        }else{
          tableColumnMapping += (tableName -> Set(identifier.names.asScala(0)))
        }
      }else{
        val alias:String = identifier.names.asScala(0)
        val name:String = identifier.names.asScala(1)
        val matchingTableName:Option[String] = aliasTableMapping.get(alias)
        if(inputTables.contains(identifier.names.asScala(1)) || outputTables.contains(identifier.names.asScala(1)) ||
          allAlias.contains(identifier.names.asScala(1))){
          // do nothing
        }
        else if(matchingTableName.isDefined){
          val existingElements:Option[Set[String]] =  tableColumnMapping.get(matchingTableName.get)
          if (existingElements.isDefined){
            tableColumnMapping += (matchingTableName.get -> (existingElements.get + identifier.names.asScala(1)))
          }else{
            tableColumnMapping += (matchingTableName.get -> Set(identifier.names.asScala(1)))
          }
        }
        else {
          // do nothing
        }
      }

    }

    /** Records an identifier as the name of an input table */
    private def visitInputIdentifier(id: SqlIdentifier): Unit = {
      inputTables += id.toString
    }

    /** Records an identifier as the name of an output table */
    private def visitOutputIdentifier(id: SqlIdentifier): Unit = {
      outputTables += id.toString
    }


    /**
      * In a CREATE TABLE statement, we record the name of the created
      * table as an output table.
      */
    private def visitCreateTable(createTable: SqlCreateTable): Unit = {
      // Visit the name of the table being created
      var tableName = StringUtils.EMPTY
      createTable.getOperandList.get(0) match {
        case id: SqlIdentifier => {
          visitOutputIdentifier(id)
          tableName = id.toString
        }
        case _ =>
      }

      // columnList
      createTable.getOperandList.get(1) match {
        case columnList:SqlNodeList => columnList.getList.asScala
          columnList.getList.asScala
            .map(node=>{
              node match {
                case sqlColumnDeclation: SqlColumnDeclaration => {
                  sqlColumnDeclation.getOperandList.get(0) match  {
                    case sqlIdentifier: SqlIdentifier => visitSqlIdentifier(sqlIdentifier,tableName)
                    case _ =>
                  }
                }
                case _  =>
              }
            })
        case _ =>
      }

    }


    /**
      * In a CREATE VIEW statement, we record the name of the created
      * view as an output table.
      */
    private def visitCreateView(createView: SqlCreateView): Unit = {
      // Visit the name of the view being created
      var tableName = StringUtils.EMPTY
      createView.getOperandList.get(0) match {
        case id: SqlIdentifier => {
          visitOutputIdentifier(id)
          tableName = id.toString
        }
        case _ =>
      }

      // columnList
      createView.getOperandList.get(1) match {
        case columnList:SqlNodeList => columnList.getList.asScala
          columnList.getList.asScala
            .map(node=>{
              node match {
                case sqlColumnDeclation: SqlColumnDeclaration => {
                  sqlColumnDeclation.getOperandList.get(0) match  {
                    case sqlIdentifier: SqlIdentifier => visitSqlIdentifier(sqlIdentifier,tableName)
                    case _ =>
                  }
                }
                case _  =>
              }
            })
        case _ =>
      }
    }

    /**
      * In an INSERT statement, we record the name of the target table as
      * an output table, and the name of a source table as an input table.
      */
    private def visitInsert(insert: SqlInsert): Unit = {
      var tableName =""
      // Visit "source" node of insert statement
      insert.getSource match {
        case id: SqlIdentifier => visitInputIdentifier(id)
        case _ =>
      }

      // Visit "target" node of insert statement
      insert.getTargetTable match {
        case id: SqlIdentifier => {
          visitOutputIdentifier(id)
          tableName = id.toString
        }
        case _ =>
      }

      insert.getTargetColumnList match {
        case nodeList:SqlNodeList =>{
          visitSQLNodeList(nodeList,tableName)
        }
        case _ =>
      }
    }


   private def visitSQLNodeList(nodeList: SqlNodeList,tableName:String):
   Unit = {
      nodeList.getList.asScala.map(col=>{
        col match {
          case sqlIdentifier: SqlIdentifier => {
            visitSqlIdentifier(sqlIdentifier,tableName)
          }
          case _  =>
        }

      })
    }

    // Visit an AS statement inside a SELECT statement
    // AS could be for column alias ? as well as for table
    private def visitAs(basic: SqlBasicCall): Unit = {
      // Each AS statement has two operands. We only care about the first (second is an alias)
      var tableName = StringUtils.EMPTY
      val importantNode = basic.getOperandList.get(0)
      val aliasNode = basic.getOperandList.get(1)
      importantNode match {
        case id: SqlIdentifier => {
          visitInputIdentifier(id)
          tableName = id.toString
        }
        case sqlBasicCall: SqlBasicCall =>
        case _ =>
      }
      aliasNode match {
        case id: SqlIdentifier => {
          if(StringUtils.isNotBlank(tableName)){
            aliasTableMapping += (id.toString->tableName)
          }
          else if(basic.getOperandList.get(0).isInstanceOf[SqlSelect]){
            aliasMapping += (id.toString -> basic.getOperandList.get(0).asInstanceOf[SqlSelect])
          }
          allAlias += id.toString
        }
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
      var tableName = ""
      select.getFrom match {
        case id: SqlIdentifier => {
          visitInputIdentifier(id)
          tableName = id.toString
        }
        case as: SqlBasicCall if as.getKind == SqlKind.AS => visitAs(as)
        case join: SqlJoin => visitJoin(join)
        case _ =>
      }

      // Helper for visiting a Join statement inside of a Select statement
      def visitJoin(join: SqlJoin): Unit = List(join.getLeft, join.getRight,join.getCondition).foreach {
        case id: SqlIdentifier => {
          visitInputIdentifier(id)
          if(id.names.size() < 2){
            // if no alias
            aliasTableMapping += (id.names.asScala(0)->id.names.asScala(0))
          }else{
            // if alias
            aliasTableMapping += (id.names.asScala(1)->id.names.asScala(0))
          }
        }
        case as: SqlBasicCall if as.getKind == SqlKind.AS => visitAs(as)
        case childJoin: SqlJoin => visitJoin(childJoin)

        // visit condition
        case condition:SqlBasicCall =>{
          val clauses:Array[SqlNode] = condition.operands
          clauses.map(clause=>{
            clause match {
              // multiple Conditions
              case sqlBasicCall: SqlBasicCall =>{
                // left condition will always be sql identifier
                sqlBasicCall.getOperandList.get(0) match {
                  case identifier: SqlIdentifier=>{
                   visitSqlIdentifier(identifier,tableName)
                  }
                  case _ =>
                }
             }
              // one condition
              case identifier: SqlIdentifier =>{
                visitSqlIdentifier(identifier,tableName)
              }

              case _ =>
            }

          })

        }

        case _ =>
      }



      select.getWhere match {

        // multiple clauses
        case sqlBasicCall: SqlBasicCall => {
          // number of clauses
          val clauses:Array[SqlNode] = sqlBasicCall.operands
          clauses.map(clause=>{

            clause match {

              case sqlBasicCall: SqlBasicCall =>{
                // left condition will always be sql identifier
                sqlBasicCall.getOperandList.get(0) match {
                  case identifier: SqlIdentifier=>{
                    visitSqlIdentifier(identifier,tableName)
                  }
                  case _ =>
                }

              }

              // one condition
              case identifier: SqlIdentifier =>{
                visitSqlIdentifier(identifier,tableName)
              }

              case _ =>
            }

          })
        }


        case _ =>

      }

      // Visit the nodes in select statement
      select.getSelectList match {
        case nodeList: SqlNodeList => {
          val nodes = nodeList.getList.asScala
          // tableName is an alias. Get last visited alias
          if(StringUtils.isBlank(tableName)){
            tableName = if(allAlias.nonEmpty){
              allAlias.last
            }else{
              ""
            }
          }
          nodes.toList.map(node=>{
            node match {
              case identifier:SqlIdentifier => {
                visitSqlIdentifier(identifier,tableName)
              }
              // alias used in columns
              case sqlBasicCall:SqlBasicCall => {
                sqlBasicCall.getKind  match  {
                  case SqlKind.AS => {
                    val columnNode = sqlBasicCall.getOperandList.get(0)
                    val aliasNode = sqlBasicCall.getOperandList.get(1)

                    columnNode match {
                      case identifier: SqlIdentifier => {
                        visitSqlIdentifier(identifier,tableName)
                      }
                      case _ =>
                    }
                    aliasNode match {
                      case id: SqlIdentifier => {
                        allAlias += id.toString
                      }
                      case _ =>
                    }

                  }

                  case _ =>

                }


              }

              case _  =>
            }
          })

        }
      }

    }

    /**
      * In an UPDATE statement, we record the name of the target table
      * as an output table.
      */
    private def visitUpdate(update: SqlUpdate): Unit = {
      var tableName = ""
      update.getTargetTable match {
        case id: SqlIdentifier => {
          visitOutputIdentifier(id)
          tableName = id.toString
        }
        case _ =>
      }

      update.getTargetColumnList match {
        case nodeList:SqlNodeList =>{
         visitSQLNodeList(nodeList,tableName)
        }
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
