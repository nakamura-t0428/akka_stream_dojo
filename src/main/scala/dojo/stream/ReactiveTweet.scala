package dojo.stream

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._

final case class Author(handle: String)
 
final case class Hashtag(name: String)
 
final case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags: Set[Hashtag] =
    body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
}

object ReactiveTweet extends App {
  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()
  
  val akkaTag = Hashtag("#akka")
  
  
  val authors: Flow[Tweet, Author, NotUsed] =
    Flow[Tweet]
      .filter(_.hashtags.contains(akkaTag))
      .map(_.author)
}