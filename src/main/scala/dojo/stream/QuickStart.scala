package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

object QuickStart extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  val source = Source(1 to 100)
  
  source.runForeach{ i => println(i) }
  val factorials = source.scan(BigInt(1))((acc, next) => acc*next)
  val result = factorials
    .map(num => ByteString(s"${num}\n"))
    .runWith(FileIO.toPath(Paths.get("factorials.txt")))
  Await.result(result, 1.second)
}