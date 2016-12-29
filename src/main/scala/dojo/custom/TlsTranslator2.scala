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
import java.nio.ByteBuffer
import akka.event.Logging
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLContext
import java.security._
import java.io.InputStream

class TlsTranslator2 extends GraphStage[BidiShape[ByteString, ByteString, ByteString, ByteString]] {
  val ksPassword = "password".toCharArray()
  val LINE_BUF_SIZE = 1024
  // Ciphered -> Plain
  val in1  = Inlet[ByteString]("TlsTranslator.in1")
  val out1 = Outlet[ByteString]("TlsTranslator.out1")
  // Ciphered <- Plain
  val out2 = Outlet[ByteString]("TlsTranslator.out2")
  val in2  = Inlet[ByteString]("TlsTranslator.in2")
  
  override val shape = BidiShape.of(in1, out1, in2, out2)
  
  case class TransState(i:Int)
  object TransState {
    object CHK_FIRST_LINE extends TransState(1)
    object RUNNING extends TransState(2)
    object SSL_HANDSHAKE extends TransState(3)
  }
  
  override def createLogic(attr:Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var tranState:TransState = TransState.CHK_FIRST_LINE
    var byteBuff:ByteString = ByteString.empty
    var sslContextOp:Option[SSLContext] = None
    var sslEngineOp:Option[SSLEngine] = None
    
    def getTrustManager(target:String) = {
      val ks:KeyStore = KeyStore.getInstance("PKCS12")
      val ksFile: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")
      
      require(ksFile != null, "Keystore required!")
      ks.load(ksFile, ksPassword)
      
      val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, password)
      
      val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
      tmf.init(ks)
      
    }
    
    def initSSLContext(target:String) = {
    }
    
    def connectMethod {
      val target = byteBuff.drop("CONNECT ".length()).takeWhile { c => c != ' ' && c != '\r' && c != '\n'}.decodeString("UTF-8")
      
      val sslContext = SSLContext.getDefault
      val sslEngine = sslContext.createSSLEngine()
      sslEngine.beginHandshake()
      
      println(s"target: '${target}'")
      val msg = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n"
      push(out1, ByteString(msg))
      byteBuff = ByteString.empty
      tranState = TransState.RUNNING
    }
    
    def otherMethod {
      push(out1, byteBuff)
      byteBuff = ByteString.empty
      tranState = TransState.RUNNING
      // コネクションを終了する
    }
    
    setHandler(in1, new InHandler {
      override def onPush(): Unit = {
        val v = grab(in1)
        tranState match {
          case TransState.CHK_FIRST_LINE =>
            byteBuff = byteBuff ++ v
            val i = byteBuff.indexOf('\n')
            if(i > 0 && byteBuff(i-1)=='\r') {
              // CONNECT か調査する
              if(byteBuff.startsWith("CONNECT ")) {
              // CONNECT => TLSの準備をしてOKを返す
                println("CONNECT found.")
                connectMethod
              } else {
              // それ以外 => 次へそのまま渡す
                println("Method is not CONNECT.")
                otherMethod
              }
            } else if(byteBuff.size > 2048) {
              // Reject する
              println("Rejected.")
              val msg = "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n"
              push(out2, ByteString(msg))
            } else {
              // 次を読み込む
              println("Read next chars.")
              pull(in1)
            }
          case TransState.RUNNING =>
            println("Running.")
            push(out1, v)
        }
      }
    })
    setHandler(out1, new OutHandler {
      override def onPull(): Unit = {
        pull(in1)
      }
    })
    setHandler(in2, new InHandler {
      override def onPush(): Unit = {
        val v = grab(in2)
        tranState match {
          case TransState.CHK_FIRST_LINE =>
            pull(in2)
          case TransState.RUNNING =>
            push(out2, v)
        }
      }
    })
    setHandler(out2, new OutHandler {
      override def onPull(): Unit = {
        pull(in2)
      }
    })
  }
}
