package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

object ConstructingGraphs extends App {
  implicit val system = ActorSystem("StreamGraphs")
  implicit val materializer = ActorMaterializer()
  
  val g = RunnableGraph.fromGraph(GraphDSL.create() {implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val in = Source(1 to 10)
    val out = Sink.foreach[Int](println(_))
    
    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))
    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
    
    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
                bcast ~> f4 ~> merge
    
    ClosedShape
  })
  g.run()
}
