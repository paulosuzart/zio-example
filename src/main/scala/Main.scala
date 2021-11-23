import zhttp.http.*
import zhttp.service.*
import zhttp.service.Server
import zio.console.*
import zio.ZIO
import zio.*

import java.io.IOException
import alleycats.std.set.*
import cats.*
import cats.data.*
import cats.implicits.*
import cats.syntax.semigroup.*
import cats.syntax.traverse.*
import cats.syntax.applicative.*
import io.bpp.taskqueue.TaskQueue
import zio.duration._

import scala.language.higherKinds
import scala.annotation.tailrec


val runtime = zio.Runtime.default

/**
 * Solves the puzzle that returns the first repeated letter in a given word
 */
object Solver:

  // recursively searches for the letter that is already present in a set
  @tailrec private def check[F[_], A: Eq](word: List[A], container: F[A])
                                         (using n: Applicative[F],
                                          c: Semigroup[F[A]],
                                          m: Traverse[F])
  : Option[A] = word match {
    case x :: _ if m.exists(container) {
      _ === x
    } => x.some
    case x :: xs => check(xs, container |+| n.pure(x))
    case Nil => none
  }


  // just isolates the logic so return is trnslated into a presentable string
  private def doCheck(word: List[Char]): zio.UIO[String] =
    ZIO.effectTotal(check(word.toList, Set.empty[Char]) match {
      case Some(a) => s"Found repeated '${a}'!"
      case None => "No repetition found :("
    })

  // the actual program witht the effects
  def solve(word: String) =
    for {
      res <- doCheck(word.toList)
    } yield res


object HelloWorld extends App :
  // Create HTTP route
  val app = HttpApp.collectM {
    case req@Method.GET -> Root / "find_repeated" => req.url.queryParams.get("word") match {
      case Some(w :: _) => Solver.solve(w).map(Response.text(_))
      case _ => UIO(Response.fromHttpError(HttpError.BadRequest("No word provided")))
    }
  }

  // Run it like any simple app
  override def run(args: List[String]) =
    TaskQueue.app()
      .provideSomeLayer(TaskQueue.consumer ++ zio.console.Console.live)
      .exitCode
