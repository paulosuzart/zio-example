import zhttp.http._
import zhttp.service._
import zhttp.service.Server

import zio.console._
import zio.ZIO
import zio._
import java.io.IOException

import scala.annotation.tailrec

val runtime = zio.Runtime.default

/**
 * Solves the puzzle that returns the first repeated letter in a given word
 */
object Solver:

  // recursively searches for the letter that is already present in a set
  @tailrec private def check(word: List[Char], container: Set[Char] = Set.empty)
  : Option[Char] = word match {
    case x :: _ if container contains x => Some(x)
    case x :: xs => {
      val newSet = container + x
      check(xs, newSet)
    }
    case Nil => None
  }

  // just isolates the logic so return is trnslated into a presentable string
  private def doCheck(word: List[Char]) : zio.UIO[String] =
    ZIO.effectTotal(check(word.toList) match {
      case Some(a) => s"Found repeated '${a}'!"
      case None => "No repetition found :("
    })

  // the actual program witht the effects
  def solve(word : String) =
    for {
      res <- doCheck(word.toList)
    } yield res


object HelloWorld extends App:
  // Create HTTP route
  val app = HttpApp.collectM {
    case req @ Method.GET -> Root / "find_repeated" => req.url.queryParams.get("word") match {
      case Some(w :: _) => Solver.solve(w).map(Response.text(_))
      case _ => UIO(Response.fromHttpError(HttpError.BadRequest("No word provided")))
    }
  }

  // Run it like any simple app
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app.silent).exitCode
