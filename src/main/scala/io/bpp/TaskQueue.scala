package io.bpp

import zio._
import zio.clock.Clock
import zio.duration._
import zio.ZLayer, zio.ZManaged, zio.blocking.Blocking, zio.clock.Clock, zio.console.putStrLn, zio.Ref
import zio.stream.ZTransducer
import zio.kafka.consumer.{ Consumer, ConsumerSettings, CommittableRecord }
import zio.kafka.consumer._
import zio.kafka.serde._

package taskqueue {

  import zio.stream.ZSink

  import java.time.Instant
  case class Leasable(id : String, nextLeaseAt : Instant, currentAttemp: Int, leased : Boolean)
  sealed trait TaskQueueCommand

  case class Offer(id: String, namespace: String, payload : Array[Byte])
  case class Lease(namespaces: List[String], batchSize: Int)
  case class Ack(id: String)
  case class Entry(key: String, v: String)

  object TaskQueue {
    val settings: ConsumerSettings =
      ConsumerSettings(List("localhost:9092", "localhost:9093"))
        .withGroupId("group")
        .withClientId("client")
        .withCloseTimeout(30.seconds)

    val consumerManaged: ZManaged[Clock with Blocking, Throwable, Consumer] =
      Consumer.make(settings)

    val consumer: ZLayer[Clock with Blocking, Throwable, Has[Consumer]] =
      ZLayer.fromManaged(consumerManaged)


    // Safe in Concurrent Environment
    def request(c: CommittableRecord[String, String], counter: Ref[Set[Entry]]) = {
      for {
        _ <- counter.modify(current => (current + Entry(c.record.key, c.record.value), current + Entry(c.record.key, c.record.value)))
        nrn <- counter.get
        _ <- putStrLn(s"Set now contains ${nrn}")
      } yield c.offset
    }

    def run(counter: Ref[Set[Entry]]) = Consumer.subscribeAnd(Subscription.topics("leases"))
      .plainStream(Serde.string, Serde.string)
      .tap(cr => putStrLn(s"Record key: ${cr.record.key}, value: ${cr.record.value}"))
      .tap(cr => request(cr, counter))
      .map(cf => cf.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapM(_.commit)
      .runDrain
    
    def logger(counter: Ref[Set[Entry]]) = for {
      x <- counter.get
      _ <- putStrLn(s"Current Set: ${x}")
    } yield ()
    
    def app() = {
      val spaced = Schedule.spaced(5.seconds)
      for {
      c <- Ref.make(Set.empty[Entry])
      fiber <- logger(c).schedule(spaced).fork
      _ <- run(c)
      _ <- fiber.join 
    } yield ()
    }

  }

}


