package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

object TimeBasedProcessing extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  
  def lineSink(filename:String) =
    Flow[String].map(s => ByteString(s + "\n"))
                .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
      
  val source = Source(1 to 100)
  
  val factorials = source.scan(BigInt(1))((acc, next) => acc*next)
  val done: Future[Done] =
    factorials
      .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
      .runForeach(println)
  
  Await.result(done, 200.second)
}