package com.ovoenergy.effect

import cats.FlatMap
import cats.effect.Async
import cats.syntax.applicative._
import fs2.{Pipe, Scheduler, Stream}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

/**
 * A type class that encodes the ability of some effect F
 * to represent a delay in the processing of subsequent steps
 */
trait Delay[F[_]] {
  def delay[A](duration: FiniteDuration): Pipe[F, A, A]
  def sleep_[A](duration: FiniteDuration): Stream[F, Nothing]
}

object Delay {
  type StreamDelay[F[_]] = Stream[F, Scheduler]

  implicit def AsyncDelay[F[_]: Async: FlatMap](
    implicit scheduler: Scheduler,
    ec: ExecutionContext
  ): Delay[F] =
    new Delay[F] {
      override def delay[A](duration: FiniteDuration): Pipe[F, A, A] =
        stream =>
          for {
            element <- stream
            delayed <- scheduler.delay(Stream.eval(element.pure[F]), duration)
          } yield delayed

      override def sleep_[A](duration: FiniteDuration): Stream[F, Nothing] =
        for {
          delayed <- scheduler.sleep_(duration)
        } yield delayed
    }

  def apply[F[_]: Delay]: Delay[F] = implicitly
}
