import zio.console.*
import zio.ZIO
import zio.*

import java.io.IOException
import io.bpp.taskqueue.{TaskQueue, GlobalSet}
import zio.duration._

import scala.language.higherKinds
import scala.annotation.tailrec
import zio.blocking.Blocking
import zio.console.Console.Service
import zio.kafka.consumer.Consumer
import zio.clock.Clock

/** Solves the puzzle that returns the first repeated letter in a given word
  */
object Solver extends zio.App:

  // Run it like any simple app
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    var l1 : ZLayer[Console, Throwable, TaskQueue] = (Console.live ++ TaskQueue.storeLayer) >>> TaskQueue.live
    val env: ZLayer[Clock & Blocking & Console, Throwable, Has[Consumer] & TaskQueue] =
      TaskQueue.consumer ++ l1
    TaskQueue.app.provideCustomLayer(env).exitCode

  }
