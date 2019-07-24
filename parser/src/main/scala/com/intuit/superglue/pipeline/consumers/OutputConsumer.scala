package com.intuit.superglue.pipeline.consumers

import java.time.LocalDateTime

import com.intuit.superglue.pipeline.consumers.OutputConsumer.Event

/**
  * An OutputConsumer receives the output metadata from parsing statements
  * from a script.
  *
  * An OutputConsumer can receive events of two types: A
  * [[OutputConsumer.Message]] which
  * contains a payload of type T, or an
  * [[OutputConsumer.EndOfStream]],
  * which signals that the consumer will not receive any more events.
  *
  * Implementations may choose to buffer messages and flush them at
  * any point. An example usage of this would be to commit 50 rows to
  * a database in one transaction, rather than in 50 transactions.
  * If a consumer receives the EndOfStream event, it should flush any
  * buffered messages immediately, as it will not be called again.
  *
  * @tparam T The type of payload carried by a Message event.
  */
trait OutputConsumer[T] {
  def accept(event: Event[T]): Unit
}

object OutputConsumer {
  sealed trait Event[+T]
  case class StartOfStream(startTime: LocalDateTime) extends Event[Nothing]
  case class Message[T](payload: T) extends Event[T]
  case object EndOfStream extends Event[Nothing]
}
