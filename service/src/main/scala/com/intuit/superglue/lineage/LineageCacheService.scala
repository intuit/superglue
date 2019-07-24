package com.intuit.superglue.lineage

import scala.language.postfixOps
import pureconfig.generic.auto._
import com.intuit.superglue.dao.model.{Direction, _}
import com.google.common.cache._
import com.intuit.superglue.dao.SuperglueRepository
import com.intuit.superglue.dao.model.PrimaryKeys.TablePK

class LineageCacheService(val superglueRepository: SuperglueRepository) {

  private case class CacheKey(tablePk: TablePK, direction: Direction)

  // Guava Cache
  // As the cache size grows close to the maximum, the cache evicts entries that are less likely to be used again.
  private val lineageCache = CacheBuilder.newBuilder()
    .maximumSize(100000L)
    .build[CacheKey, Set[LineageView]]

  def getCachedLineageView(table: TablePK, direction: Direction): Option[Set[LineageView]] = {
    Option(lineageCache.getIfPresent(CacheKey(table, direction)))
  }

  private def addLineageViewToCache(table: TablePK, direction: Direction, value: Set[LineageView]): Unit = {
    lineageCache.put(CacheKey(table, direction), value)
  }

  def addLineageViewsToCache(tableNames: Set[TablePK], direction: Direction, value: Set[LineageView]): Unit = {
    val groupedLineageViewResult: Map[TablePK, Set[LineageView]] = {
      direction match {
        case Output => value.groupBy(_.outputTableId)
        case Input => value.groupBy(_.inputTableId)
      }
    }

    // tables which has no lineage for the specified direction
    val tablesNoLineage = tableNames -- groupedLineageViewResult.keys
    val lineageViewsToAdd: Map[TablePK, Set[LineageView]] = {
      if (tablesNoLineage.nonEmpty) {
        groupedLineageViewResult ++ tablesNoLineage.flatMap(t => Map(t -> Set.empty[LineageView]))
      } else {
        groupedLineageViewResult
      }
    }

    lineageViewsToAdd.foreach(g => addLineageViewToCache(g._1, direction, g._2))
  }

  def invalidateKey(table: TablePK, direction: Direction): Unit = {
    lineageCache.invalidate(CacheKey(table, direction))
  }

  def invalidateAll(): Unit = {
    lineageCache.invalidateAll()
  }
}
