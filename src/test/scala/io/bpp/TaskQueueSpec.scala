package io.bpp

import io.bpp.taskqueue.Entry
import io.bpp.taskqueue.TaskQueue
import io.bpp.taskqueue.GlobalSet
import zio.test.DefaultRunnableSpec
import zio.ZRef
import zio.test._
import zio.kafka.consumer.CommittableRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.kafka.consumer.Offset
import zio.ZIO
import zio.console.Console

import zio.test.Assertion.{isEmpty, equalTo}
import zio.test.TestAspect.sequential



object TaskQueueSpec extends DefaultRunnableSpec {
  import TaskQueue._

  val store   = TaskQueue.storeLayer
  val testEnv = (Console.live ++ store) >>> TaskQueue.live

  def spec = suite("Handle should add entry to the global map")(
    testM("Add entry to ref correctly") {
      val record            = new ConsumerRecord("topic", 0, 21, "a-key", "a-value")
      val committableRecord = CommittableRecord[String, String](record, Map.empty, None)

      for {
        _ <- handle(committableRecord)
        x <- ZIO.access[GlobalSet](_.get)
        c <- x.get
      } yield assert(c)(equalTo(Map("a-key" -> Entry("a-key", "a-value"))))
    },
    testM("Drop non existing entry from ref keeps it unchanged") {
      val record2 =
        new ConsumerRecord[String, String]("topic", 0, 21, "a-key2", null.asInstanceOf[String])
      val tombstone = CommittableRecord[String, String](record2, Map.empty, None)

      for {
        _ <- handle(tombstone)
        x <- ZIO.access[GlobalSet](_.get)
        c <- x.get
      } yield assert(c)(equalTo(Map("a-key" -> Entry("a-key", "a-value"))))
    },
    testM("Drop entry from ref correctly") {
      val record2 =
        new ConsumerRecord[String, String]("topic", 0, 21, "a-key", null.asInstanceOf[String])
      val tombstone = CommittableRecord[String, String](record2, Map.empty, None)

      for {
        _ <- handle(tombstone)
        x <- ZIO.access[GlobalSet](_.get)
        c <- x.get
      } yield assert(c)(isEmpty)
    }
  ).provideCustomLayerShared(store ++ testEnv) @@ sequential

}
