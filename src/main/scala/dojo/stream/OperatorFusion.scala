package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

object OperatorFusion extends App {
  implicit val system = ActorSystem("FlowsAndBasics")
  implicit val materializer = ActorMaterializer()
  
  val flow = Flow[Int].map(_ * 2).filter(_ > 500)
  val fused = Fusing.aggressive(flow)
  
  val source = Source.fromIterator { () => Iterator from 0 }
    .via(fused)
    .take(1000)
  
  val sink = Sink.foreach[Int](println(_))
  val res = source.runWith(sink)
  
  Await.ready(res, 10.seconds)
}