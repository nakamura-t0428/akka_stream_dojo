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


case class BidiSwitcherShape[In, Out](
    srcIn:   Inlet[In],
    srcOut:  Outlet[Out],
    dst1In:  Inlet[In],
    dst1Out: Outlet[Out],
    dst2In:  Inlet[In],
    dst2Out: Outlet[Out]
    ) extends Shape {
  override val inlets:scala.collection.immutable.Seq[Inlet[_]]  = srcIn::dst1In::dst2In::Nil
  override val outlets:scala.collection.immutable.Seq[Outlet[_]] = srcOut::dst1Out::dst2Out::Nil
  override def deepCopy() = BidiSwitcherShape(
      srcIn.carbonCopy,
      srcOut.carbonCopy,
      dst1In.carbonCopy,
      dst1Out.carbonCopy,
      dst2In.carbonCopy,
      dst2Out.carbonCopy
      )
  override def copyFromPorts(
      inlets:scala.collection.immutable.Seq[Inlet[_]],
      outlets:scala.collection.immutable.Seq[Outlet[_]]) = {
    assert(inlets.size == this.inlets.size)
    assert(outlets.size == this.outlets.size)
    BidiSwitcherShape[In, Out](
        inlets(0).as[In],
        outlets(0).as[Out],
        inlets(1).as[In],
        outlets(1).as[Out],
        inlets(2).as[In],
        outlets(2).as[Out]
        )
  }
}

class HttpsChanger extends GraphStage[BidiSwitcherShape[ByteString,ByteString]] {
  val srcIn  = Inlet[ByteString]("HttpsChanger.srcIn")
  val srcOut = Outlet[ByteString]("HttpsChanger.srcOut")
  val httpIn  = Inlet[ByteString]("HttpsChanger.httpIn")
  val httpOut = Outlet[ByteString]("HttpsChanger.httpOut")
  val httpsIn  = Inlet[ByteString]("HttpsChanger.httpsIn")
  val httpsOut = Outlet[ByteString]("HttpsChanger.httpsOut")
  
  override val shape = BidiSwitcherShape(srcIn, srcOut, httpIn, httpOut, httpsIn, httpsOut)
  sealed case class ProxyState(state:Int)
  object ProxyState {
    object UNKNOWN extends ProxyState(0)
    object HTTP  extends ProxyState(1)
    object HTTPS_PRE  extends ProxyState(2)
    object HTTPS extends ProxyState(3)
  }
  
  
  override def createLogic(attr:Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var state:ProxyState = ProxyState.HTTP
    
    setHandler(srcIn, new InHandler {
      override def onPush():Unit = {
        val v = grab(srcIn)
        // Check And Change State
        // make SSL connection when CONNECT method.
        state match {
          case ProxyState.HTTP => push(httpOut, v)
          case ProxyState.HTTPS => push(httpsOut, v)
          case _ => pull(srcIn)
        }
      }
    })
    setHandler(srcOut, new OutHandler{
      override def onPull(): Unit = {
        state match {
          case ProxyState.HTTP => pull(httpIn)
          case ProxyState.HTTPS => pull(httpsIn)
          case _ => {}
        }
      }
    })
    setHandler(httpIn, new InHandler {
      override def onPush():Unit = {
        val v = grab(httpIn)
        state match {
          case ProxyState.HTTP => push(srcOut, v)
          case _ => pull(httpIn)
        }
      }
    })
    setHandler(httpOut, new OutHandler{
      override def onPull(): Unit = {
        state match {
          case ProxyState.HTTP => pull(srcIn)
          case _ => {}
        }
      }
    })
    setHandler(httpsIn, new InHandler {
      override def onPush():Unit = {
        val v = grab(httpsIn)
        state match {
          case ProxyState.HTTPS => push(srcOut, v)
          case _ => pull(httpsIn)
        }
      }
    })
    setHandler(httpsOut, new OutHandler{
      override def onPull(): Unit = {
        state match {
          case ProxyState.HTTPS => pull(srcIn)
          case _ => {}
        }
      }
    })
  }
}
