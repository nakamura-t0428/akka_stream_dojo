package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

object ReusablePieces extends App{
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  
  def lineSink(filename:String) =
    Flow[String].map(s => ByteString(s + "\n"))
                .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
      
  val source = Source(1 to 100)
  
  val factorials = source.scan(BigInt(1))((acc, next) => acc*next)
  
  val result = factorials.map(_.toString).runWith(lineSink("factorial2.txt"))
  Await.result(result, 1.second)
}