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

object ConstructingPartialGraphs extends App {
  implicit val system = ActorSystem("StreamGraphs")
  implicit val materializer = ActorMaterializer()
  
  val pickMaxOfThree = GraphDSL.create(){ implicit b =>
    import GraphDSL.Implicits._
    
    val zip1 = b.add(ZipWith[Int, Int, Int](math.max _))
    val zip2 = b.add(ZipWith[Int, Int, Int](math.max _))
    
    zip1.out ~> zip2.in0
    
    UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
  }
  
  val resultSink = Sink.head[Int]
  
  val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) {implicit b => sink =>
    import GraphDSL.Implicits._
    
    val pm3 = b.add(pickMaxOfThree)
    
    Source.single(1) ~> pm3.in(0)
    Source.single(2) ~> pm3.in(1)
    Source.single(3) ~> pm3.in(2)
    
    pm3.out ~> sink.in
    
    ClosedShape
  })
  g.run().foreach{println _}
  
  
  val pairsGraph = GraphDSL.create(){ implicit b =>
    import GraphDSL.Implicits._
    
    val zip = b.add(Zip[Int, Int]())
    def ints = Source.fromIterator { () => Iterator.from(1) }
    ints.filter( _ % 2 != 0) ~> zip.in0
    ints.filter( _ % 2 == 0) ~> zip.in1
    
    SourceShape(zip.out)
  }
  
  val pairs = Source.fromGraph(pairsGraph)
  val firstPair = pairs.runWith(Sink.foreach { println _ })
}
