package server

import org.json4s.ShortTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._

trait Jsonmsg

case class Jtweet(content: String) extends Jsonmsg

case class Jfollower(uid: String) extends Jsonmsg

case class Jsent(content: String) extends Jsonmsg

object Jsonmsg {
  val jsonmsgs = List[Jsonmsg](
  )

  private implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[Jtweet], classOf[Jfollower],classOf[Jsent])))
  def toJson(jsonmsgs: List[Jsonmsg]): String = writePretty(jsonmsgs)
  def toJson(jsonmsg: Jsonmsg): String = writePretty(jsonmsg)
}