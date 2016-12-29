package dojo.custom

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
import akka.stream.TLSProtocol._
import akka.stream.scaladsl.{TLSPlacebo, TLS}

class TlsTranslator extends GraphStage[BidiShape[ByteString, SslTlsInbound, SslTlsOutbound, ByteString]] {
  val tcpBidi = BidiFlow.fromFunctions[ByteString,ByteString,ByteString,ByteString](a=>a, a=>a)
  val dstBidi = BidiFlow.fromFunctions[SslTlsInbound,SslTlsInbound,SslTlsOutbound,SslTlsOutbound](a=>a, a=>a)
  // Ciphered (ByteString)
  val tcpIn1    = tcpBidi.shape.in1
  val tcpOut1 = tcpBidi.shape.out1
  val tcpOut2   = tcpBidi.shape.out2
  val tcpIn2 = tcpBidi.shape.in2
  // Plain (SslTlsInbound, SslTlsOutbound)
  val dstIn1    = dstBidi.shape.in1
  val dstOut1 = dstBidi.shape.out1
  val dstOut2   = dstBidi.shape.out2
  val dstIn2 = dstBidi.shape.in2
  
  override val shape = BidiShape.of(tcpIn1, dstOut1, dstIn2, tcpOut2)
  
  override def createLogic(attr:Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var sslOp:Option[BidiFlow[ByteString, SslTlsInbound, SslTlsOutbound, ByteString, NotUsed]] = None
    
    override def preStart(): Unit = {
      pull(tcpIn1)
    }
  
    def setPracebo() = {
      val ssl = TLSPlacebo().reversed
      sslOp = Some(ssl)
      tcpBidi.atop(ssl).atop(dstBidi)
    }
    setPracebo()
    
    setHandler(tcpIn1, new InHandler {
      override def onPush():Unit = {
        val v = grab(tcpIn1)
        push(tcpOut1, v)
      }
    })
    setHandler(tcpOut1, new OutHandler {
      override def onPull():Unit = {
        pull(tcpIn1)
      }
    })
    setHandler(tcpIn2, new InHandler {
      override def onPush():Unit = {
        val v = grab(tcpIn2)
        push(tcpOut2, v)
      }
    })
    setHandler(tcpOut2, new OutHandler {
      override def onPull():Unit = {
        pull(tcpIn2)
      }
    })
    
    setHandler(dstIn1, new InHandler {
      override def onPush():Unit = {
        val v = grab(dstIn1)
        push(dstOut1, v)
      }
    })
    setHandler(dstOut1, new OutHandler {
      override def onPull():Unit = {
        pull(dstIn1)
      }
    })
    setHandler(dstIn2, new InHandler {
      override def onPush():Unit = {
        val v = grab(dstIn2)
        push(dstOut2, v)
      }
    })
    setHandler(dstOut2, new OutHandler {
      override def onPull():Unit = {
        pull(dstIn2)
      }
    })
  }
}
