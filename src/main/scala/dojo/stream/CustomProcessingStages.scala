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
import akka.stream.stage._

object CustomProcessingStages extends App {
  implicit val system = ActorSystem("StreamGraphs")
  implicit val materializer = ActorMaterializer()
  
  val doubler = Flow[Int].map{ _ * 2 }
  
  
  class FlowChanger extends GraphStage[UniformFanOutShape[Int, Int]] {
    val in = Inlet[Int]("FlowChanger.in")
    val out1 = Outlet[Int]("FlowChanger.out1")
    val out2 = Outlet[Int]("FlowChanger.out2")
    
    override val shape = UniformFanOutShape(in, out1, out2)
    
    override def createLogic(attr:Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      var isState2 = false
      setHandler(in, new InHandler {
        override def onPush():Unit = {
          val v = grab(in)
          if(v == 10) isState2 = true
          if(isState2) push(out2, v)
          else push(out1, v)
        }
      })
      setHandler(out1, new OutHandler{
        override def onPull(): Unit = {
          if(! isState2) pull(in)
        }
      })
      setHandler(out2, new OutHandler{
        override def onPull(): Unit = {
          if(isState2) pull(in)
        }
      })
    }
  }
  
  class MapInt(f: Int => Int) extends GraphStage[FlowShape[Int, Int]] {
   
    val in = Inlet[Int]("Map.in")
    val out = Outlet[Int]("Map.out")
   
    override val shape = FlowShape.of(in, out)
   
    override def createLogic(attr: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            push(out, f(grab(in)))
          }
        })
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })
      }
  }
  
  class Map[A, B](f: A => B) extends GraphStage[FlowShape[A, B]] {
   
    val in = Inlet[A]("Map.in")
    val out = Outlet[B]("Map.out")
   
    override val shape = FlowShape.of(in, out)
   
    override def createLogic(attr: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            push(out, f(grab(in)))
          }
        })
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })
      }
  }
  
  val g = RunnableGraph.fromGraph(GraphDSL.create() {implicit b =>
    import GraphDSL.Implicits._
    val in = b.add(Source(1 to 100))
    val out = b.add(Sink.foreach[Int](println(_)))
    
    val changer = b.add(new FlowChanger)
    
    val merge = b.add(Merge[Int](2))
    
    in ~> changer.in
    changer.out(0) ~> doubler ~> merge.in(0)
    changer.out(1)            ~> merge.in(1)
    merge.out ~> out
    ClosedShape
  })
  g.run()
}