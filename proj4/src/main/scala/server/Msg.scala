package server

import spray.routing._

object Msg{
  case class PostTweet(uId : String, tweet : String)
  case class Follow(uId : String, fId : String)
  case class RouteFollow(uId : String, fId : String)
  case class Deliver(uId : String, tId : String)
  case class Update(uId : String, tId : String)
  case class GetTimeLine(uId : String,ctx : RequestContext)
  case class GetSentList(uId : String,ctx : RequestContext)
  case class RouteTimeLine(uId : String, ctx : RequestContext)
  case class RouteFollowerList(uId : String,ctx : RequestContext)
  case class RouteSentList(uId : String, ctx : RequestContext)
  case class GetFollowerList(uId : String,ctx : RequestContext)
  case class Start()
  case class FindUser(uId : String, fId : String)
  case class Print()
  case class SendBack(list : Array[String])
  case class SendBackFollower(list : Array[String])
  case class StartTweet()
  case class GetTweetNum()
  case class TweetNum(num : Int)
  case class DeleteTweet(uId: String, tId: String)
  case class DeleteTimelineTweet(fId : String, tId: String)
  case class RemoveTweet(uId : String, tId : String)
}