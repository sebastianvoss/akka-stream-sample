import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.io.StdIn
import scala.util.Random

object AkkaStreamSample extends App {

  implicit val system = ActorSystem("akka-stream-sample")

  // takes the list of transformations and materializes them in the form of org.reactivestreams.Processor instances
  implicit val materializer = ActorMaterializer()

  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val numbers = Source.fromIterator(() => Iterator.continually(Random.nextInt()))

  val route = path("random") {
    get {
      parameters('count ? 10000) { count =>
        complete(
          HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            numbers.take(count).map(n => ByteString(s"$n\n"))
          )
        )
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ {
    system.terminate()
  })

}
