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
import TLSProtocol._
import dojo.custom.TlsTranslator2


object TlsTest extends App {
  implicit val system = ActorSystem("StreamGraphs")
  implicit val materializer = ActorMaterializer()
  
  val messageProcessor = Flow.fromFunction[SslTlsInbound, SslTlsOutbound]{
    case SessionBytes(s, b) =>
      println(b)
      SendBytes(b)
    case _ => SendBytes(ByteString("Unknown\n"))
  }
  
  val g = GraphDSL.create(){ implicit b =>
    import GraphDSL.Implicits._
    val tls = b.add(new TlsTranslator2)
//    val msgProc = b.add(messageProcessor)
    tls.out1 ~> tls.in2
    
    FlowShape(tls.in1, tls.out2)
  }
//  val echo = Flow.fromFunction[ByteString, ByteString]{ x => x }
  
  val flow = Flow.fromGraph(g)
  val connServ = Tcp().bindAndHandle(flow, "127.0.0.1", 8888)
}