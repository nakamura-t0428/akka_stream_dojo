package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

object DefiningAndRunningStreams2 extends App {
  implicit val system = ActorSystem("FlowsAndBasics")
  implicit val materializer = ActorMaterializer()
  
  val source:Source[Int, NotUsed] = Source(1 to 10)
  val sink:Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
  val runnable:RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
  
  val sum1 = runnable.run()
  val sum2 = runnable.run()
  
  val res1 = Await.result(sum1, 10.seconds)
  val res2 = Await.result(sum2, 10.seconds)
  println(s"result1 = ${res1}")
  println(s"result2 = ${res2}")

}