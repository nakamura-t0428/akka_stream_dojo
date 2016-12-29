package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

object DefiningAndRunningStreams extends App {
  implicit val system = ActorSystem("FlowsAndBasics")
  implicit val materializer = ActorMaterializer()
  
  val source:Source[Int, NotUsed] = Source(1 to 10)
  val sink:Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
  val runnable:RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
  
  val sum = runnable.run()
  val sum2 = source.runWith(sink)
  
  val zeros = source.map(_ => 0)
  
  val res = Await.result(sum, 10.seconds)
  val res2 = Await.result(sum2, 10.second)
  val res3 = Await.result(zeros.runWith(sink), 10.seconds)
  println(s"result = ${res}")
  println(s"result2 = ${res2}")
  println(s"result3 = ${res3}")
}