package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContext.Implicits.global

object ConstructingGraphs2 extends App {
  implicit val system = ActorSystem("StreamGraphs")
  implicit val materializer = ActorMaterializer()
  
  val topHeadSink = Sink.head[Int]
  val bottomHeadSink = Sink.head[Int]
  val sharedDoubler = Flow[Int].map(_ * 2)
  val sharedTripler = Flow[Int].map(_ * 3)
  
  val g = RunnableGraph.fromGraph(GraphDSL.create(topHeadSink, bottomHeadSink)((_, _)) {implicit builder =>
    (topHS, bottomHS) =>
    import GraphDSL.Implicits._
    
    val broadcast = builder.add(Broadcast[Int](2))
    Source.single(1) ~> broadcast.in
    broadcast.out(0) ~> sharedDoubler ~> topHS.in
    broadcast.out(1) ~> sharedTripler ~> bottomHS.in
    
    ClosedShape
  })
  g.run()._1.foreach(r => println(s"top:${r}"))
  g.run()._2.foreach(r => println(s"bottom:${r}"))
//  Await.result(, 10.seconds)
}
