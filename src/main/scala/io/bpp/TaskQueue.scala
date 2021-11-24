package io.bpp

import zio._
import zio.clock.Clock
import zio.duration._
import zio.ZLayer, zio.ZManaged, zio.blocking.Blocking, zio.clock.Clock, zio.console.putStrLn,
zio.Ref
import zio.stream.ZTransducer
import zio.kafka.consumer.{Consumer, ConsumerSettings, CommittableRecord}
import zio.kafka.consumer._
import zio.kafka.serde._
import zio.console.Console

package taskqueue {
  case class Entry(key: String, v: String)

  type TaskQueue = Has[TaskQueue.Service]
  type GlobalSet = Has[Ref[Map[String, Entry]]]

  object TaskQueue {

    val storeLayer = ZRef.make(Map.empty[String, Entry]).toLayer

    trait Service {
      def handle(record: CommittableRecord[String, String]): UIO[Unit]
    }

    val any: ZLayer[TaskQueue, Nothing, TaskQueue] = ZLayer.requires[TaskQueue]

    val live: URLayer[Console & GlobalSet, TaskQueue] =
      ZLayer.fromServices[Console.Service, Ref[Map[String, Entry]], TaskQueue.Service] {
        (console: Console.Service, globalSet: Ref[Map[String, Entry]]) =>
          new Service {
            override def handle(
                c: CommittableRecord[String, String]
            ): UIO[Unit] = for {
              _ <- globalSet.update(current =>
                (
                  current + (c.record.key -> Entry(c.record.key, c.record.value))
                )
              )
              set <- globalSet.get
              _   <- console.putStrLn(s"Current entries: ${set}").orDie
            } yield ()
          }
      }

    def handle(record: CommittableRecord[String, String]): URIO[TaskQueue, Unit] =
      ZIO.accessM(_.get.handle(record))

    val settings: ConsumerSettings =
      ConsumerSettings(List("localhost:9092", "localhost:9093"))
        .withGroupId("group")
        .withClientId("client")
        .withCloseTimeout(30.seconds)

    val consumerManaged: ZManaged[Clock with Blocking, Throwable, Consumer] =
      Consumer.make(settings)

    val consumer: ZLayer[Clock & Blocking, Throwable, Has[Consumer]] =
      ZLayer.fromManaged(consumerManaged)

    val run: ZIO[Has[Consumer] & Console & TaskQueue & Clock, Throwable, Unit] = Consumer
      .subscribeAnd(Subscription.topics("leases"))
      .plainStream(Serde.string, Serde.string)
      .tap(cr => putStrLn(s"Record key: ${cr.record.key}, value: ${cr.record.value}"))
      .tap(TaskQueue.handle(_))
      .map(cf => cf.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapM(_.commit)
      .runDrain

    val logger: URIO[GlobalSet & Console, Unit] = for {
      x <- ZIO.access[GlobalSet](_.get)
      _ <- putStrLn(s"Current Set: ${x}").orDie
    } yield ()

    val app: ZIO[Has[Consumer] & TaskQueue & GlobalSet & Console & Clock, Throwable, Unit] = {
      val spaced = Schedule.spaced(5.seconds)
      for {
        fiber <- logger.schedule(spaced).fork
        _     <- run
        _     <- fiber.join
      } yield ()
    }

  }

}
