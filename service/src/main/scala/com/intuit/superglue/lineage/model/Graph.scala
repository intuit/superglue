package com.intuit.superglue.lineage.model

case class Graph(
  nodes: Set[Node],
  links: Set[Link],
)

object Graph {
  def empty: Graph = Graph(Set.empty[Node], Set.empty[Link])
}
