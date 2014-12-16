package server

//package com.softwaremill.spray.server

import java.util.concurrent.ConcurrentHashMap
import server.Msg._

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props, actorRef2Scala}
import akka.routing.SmallestMailboxPool
import spray.routing._

import scala.Array._
import scala.collection.JavaConversions._
import scala.collection.concurrent.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Lock
import scala.concurrent.duration.Duration
import scala.math.BigInt.int2bigInt
import scala.math._
import scala.util.Random

object Server2 extends App with SimpleRoutingApp  {

  class User(var userId : String){
    val uId = userId
    val tIdList = ArrayBuffer.empty[String]
    val fIdList = ArrayBuffer.empty[String]
    val sentIdList = ArrayBuffer.empty[String]
    val followList = ArrayBuffer.empty[String]

    def addSentTweet(newtid : String) = {
      sentIdList.append(newtid)
    }
    def addFollower(newfid : String) = {
      fIdList.append(newfid)
    }

    def addTweet(newtid : String) = {
      tIdList.append(newtid)
    }

    def getFollowerList() : Array[String] = {
      return fIdList.toArray
    }

    def getSentList() : Array[String] = {
      sentIdList.toArray
    }

    def getTimeLine() : Array[String] = {
      return tIdList.toArray
    }
    def deleteTweet(tId: String) {
      tIdList -= tId
      sentIdList -= tId
    }
    def deleteTimeTweet(tId : String) {
      tIdList -= tId
    }
  }

  var ip : String = args(2)
  var startIdNum : Int = 0
  var partNum : Int = 0
  var router : ActorRef = null
  val workerMap : Map[String, ActorRef] = new ConcurrentHashMap[String, ActorRef]()
  var tweetMap : Map[String, String] = new ConcurrentHashMap[String, String]()
  var lock : Lock = new Lock()
  var second = Duration.create(1, "millis")
  var tenMili = Duration.create(1, "millis")
  var userNum : Int = 0

  val system = ActorSystem("Server")
  val master = system.actorOf(Props(new Master(args(0).toInt, args(1).toInt)), "master")
  master ! Start()

  //
  //  case "GetTweetNum" => Client ! GetTweetNum
  //  case "GetTimeLine" => Client ! GetTimeLine(args(4))
  //  case "GetSentList" => Client ! GetSentList(args(4))
  //  case "GetFollowerList" => Client ! GetFollowerList(args(4))
  //  case "RemoveTweet" => Client ! RemoveTweet(args(4), args(5))
  //  case "Follow" => Client ! Follow(args(4), args(5))


  //      val Client = system.actorOf(Props(new Client), "client")
  //      Client ! GetTweetNum
  //    }
  //    else{
  //      val clientSystem = ActorSystem("Client", ConfigFactory.parseString("""
  //          akka {
  //          #log-config-on-start = off
  //            actor {
  //            provider = "akka.remote.RemoteActorRefProvider"
  //            }
  //          remote {
  //            enabled-transports = ["akka.remote.netty.tcp"]
  //            netty.tcp {
  //              hostname = ""
  //              port = 8889
  //            }
  //          }
  //        }
  //                                                                         """ ))
  //      ip = args(0)
  //      userNum = args(2).toInt
  //      if(args(3) == "flood"){
  //        val router = clientSystem.actorOf(BroadcastPool(args(1).toInt).props(Props[Client]), "router")
  //        router ! StartTweet
  //      }
  //      else{
  //        val Client = clientSystem.actorOf(Props(new Client), "client")
  //        args(3) match {
  //        }
  //      }
  //    }


  class Master(serverNum: Int, clientNum : Int) extends Actor {
    var tweetId : BigInt = 0
    var workerSet : Array[ActorRef] = new Array[ActorRef](serverNum )
    var lock : Lock = new Lock()

    var routees : Vector[ActorRef] = null

    def initServer() {
      println("initServer")
      partNum = clientNum / serverNum
      var startId: Int = 0
      var endId: Int = 0
      var x: Int = 0
      //      for(x <- 0 to serverNum - 1) {
      //        workerSet(x) = context.actorOf(Props(new Worker(startId.toString, partNum, clientNum)).withDispatcher("BalancingDispatcher"), "Server" + x.toString)
      //        startId += partNum
      //      }
      router = context.actorOf(SmallestMailboxPool(serverNum).props(Props(new Worker(clientNum))), "router")
      println(router.path)
      //   router = context.actorOf(
      //      Props(new Worker(clientNum)).withRouter(RandomRouter(clientNum)), name = "workerRouter")
      implicit val actorSystem = ActorSystem()
      Thread.sleep(1000L)
      //
      //      for (abc <- 0 to 250) {
      //      context.self ! PostTweet("1", "buckeroo")
      //    }
      //      router ! "fuck"
      //      router ! "fuck"
      //      router ! "fuck"
      //      router ! "fuck"
      //      router ! "fuck"

      //      router ! Deliver("877", "buckero7o")
      //      router ! Deliver("878", "buckero8o")
      //     router ! RouteTimeLine(uId, ctx)
      startServer(interface = ip, port = 8091) {
        get {
          path("gettweetnum") {
              println("gettweetnum")
            complete (tweetMap.size.toString)
          }
        }~
          get {
            path("gettimeline" / Segment) { uId=>ctx=>
              println("gettimeline:"+uId)
              router ! RouteTimeLine(uId, ctx)
            }
          }~
          get {
            path("getsentlist" / Segment) { uId=>ctx=>
              router ! RouteSentList(uId, ctx)
              println("getsentlist:"+uId)
            }
          }~
          get {
            path("getfollowerlist" / Segment) { uId=>ctx=>
              router ! RouteFollowerList(uId, ctx)
              println("getfollowerlist:"+uId)
            }
          }~
          post {
            path("posttweet" / Segment / Segment ) { (uId, tweet) => //PostTweet(uId, tweet)

              var tId = tweet.hashCode().toString
              println("posttweet:"+uId+" "+tId)
              tweetMap.put(tId, uId + ": " + tweet)
              router ! Deliver(uId.toString, tId)
              complete (
                "Ok"
              )
            }
          }~
          post {
            path("follow" / Segment / Segment ) { (uId, fId) => //uid,fid
              println("follow:"+uId+" "+fId)
              router ! RouteFollow(uId, fId)
              complete (
                "Ok"
              )
            }
          }~
          post {
            path("deletetweet" / Segment / Segment ) { (uId, tId) => //uid,fid
              println("deletetweet"+uId+" "+tId)
              router ! DeleteTweet(uId, tId)
              complete (
                "Ok"
              )
            }
          }

      }

      //      router = {
      //        val routees = Vector.fill(5) {
      //        val r = context.actorOf(Props(new Worker(startId.toString, partNum, clientNum)).withDispatcher("my-balanced-dispatcher"), "Server" + x.toString)
      //        x += 1
      //        startId += partNum
      //        context watch r
      //        ActorRefRoutee(r)
      //        }
      //        Router(RoundRobinRoutingLogic(), routees)
      //      }
    }


    def createTIdForTweet(tweet : String) : String = {
      var tId : String = tweetId.toString
      tweetId += 1
      return tId
    }

    def receive = {
      case PostTweet(uId, tweet) => {
        var tId = tweet.hashCode().toString
        tweetMap.put(tId, uId + ": " + tweet)
        router ! Deliver(uId, tId)
      }

      case Follow(uId, fId) => {
        router ! RouteFollow(uId, fId)
      }

      case GetTweetNum => {
        sender ! TweetNum(tweetMap.size)
      }

      case GetTimeLine(uId,ctx) => {
        router ! RouteTimeLine(uId, ctx)
      }

      case GetSentList(uId,ctx) => {
        router ! RouteSentList(uId, ctx)
      }

      case GetFollowerList(uId,ctx) => {
        router ! RouteFollowerList(uId, ctx)
      }
      case Start() => {
        initServer()
      }

      case TweetNum(i) => {
        println(i)
      }

      case DeleteTweet(uId, tId) => {
        router ! DeleteTweet(uId, tId)
      }
    }
  }

  class Worker(clientNum : Int) extends Actor {
    var userList : ArrayBuffer[User] = new ArrayBuffer[User]()
    var startId : String = ""
    override def preStart(): Unit = {
      lock.acquire
      startId = startIdNum.toString
      workerMap.put(startId, self)
      startIdNum += partNum
      lock.release
      initUserList
    }
    println("Server init done")
    def initUserList() {
      var start = startId.toInt
      var end = start + partNum
      println(partNum)
      for(uId <- start until end){
        //        println(uId)
        var user = new User(uId.toString)
        initFriendList(user)
        userList.append(user)
      }
    }

    def initFriendList(user : User) = {
      var friendNum : Int = 0
      var percent = user.uId.toDouble / clientNum.toDouble
      var range : collection.immutable.Range.Inclusive = null
      if(percent < 0.8)
        range = 1 to 50
      else if(percent < 0.87 )
        range = 51 to 100
      else if(percent < 0.95)
        range = 101 to 500
      else
        range = 501 to 1000
      friendNum = range(Random.nextInt(range.length))

      if(friendNum < 100){
        for(i <- 1 to friendNum){
          var friend = Random.nextInt(clientNum).toString
          while(user.fIdList.contains(friend) || friend == user.uId ){
            friend = Random.nextInt(clientNum).toString
          }
          user.fIdList.append(friend)
        }
      }
      // else{
      //   var friendId = Random.nextInt(clientNum)
      //   if(friendId + friendNum > clientNum)
      //     friendId -= friendNum
      //   for(friend <- friendId to friendId + friendNum)
      //     user.fIdList.append(friend.toString())
      // }

    }
    def print() {
      for(u <- userList ) {
        println(context.self.path, u.uId )
      }
    }
    def getUserByUserId(uId : String) : User = {
      var num = uId.toInt - startId.toInt
      return userList(num)
    }

    def findUser(uId : String) : ActorSelection = {
      var n = (uId.toInt / partNum).toString
      var serverId = context.actorSelection("../Server" + n)
      return serverId
    }
    def containUserByUserId(uId : String) : Boolean = {
      var _uId : Int = uId.toInt
      var _startId : Int = startId.toInt
      if(_uId >= _startId && _uId < _startId + partNum){
        return true
      } else {
        return false
      }
    }

    def addToTweetList(uId : String, tId : String) = {
      var user = getUserByUserId(uId)
      user.addTweet(tId)
      user.addSentTweet(tId)
      var userFIdList = user.getFollowerList
      for(fId <- userFIdList) {
        var server : ActorRef = findServer(fId)
        server ! Update(fId, tId)
      }
    }

    def findServer(uId : String) : ActorRef = {
      //      var result : ActorRef  = null
      //      for(key <- workerMap.keySet ){
      //        val diff = uId.toInt - key.toInt
      //        if(diff >= 0 && diff < partNum)
      //          result = workerMap.get(key).get
      //      }
      //      return result
      workerMap.filterKeys(key => uId.toInt - key.toInt >= 0 &&  uId.toInt - key.toInt < partNum).head._2
    }

    def sendBackToClient(flist : Array[String], client : RequestContext) = {
      //      if(flist.size < 15)
      //            client ! SendBack(flist)
      //          else{
      //            var copyStart = 0
      //            while(copyStart < flist.size - 15){
      //              var tempList  = new Array[String](15)
      //              Array.copy(flist, copyStart, tempList, 0, 15)
      //              client ! SendBack(tempList.toArray)
      //              copyStart += 15
      //            }
      //            client ! SendBack(flist.drop(copyStart))
      //          }
    }

    def receive = {

      case DeleteTimelineTweet(uId, tId) => {
        getUserByUserId(uId).deleteTimeTweet(tId)
      }
      case DeleteTweet(uId, tId) => {
        if(containUserByUserId(uId)) {
          getUserByUserId(uId).deleteTweet(tId)
          var fList = getUserByUserId(uId).getFollowerList
          for( fId <- fList) {
            var server = findServer(fId)
            server ! DeleteTimelineTweet(fId, tId)
          }
          tweetMap -= tId
        } else {
          var server = findServer(uId)
          server ! DeleteTweet(uId, tId)
        }
      }
      case RouteFollow(uId, fId) =>{
        if(containUserByUserId(uId)) {
          getUserByUserId(uId).fIdList.append(fId)
        }
        else{
          var server = findServer(uId)
          server ! RouteFollow(uId, fId)
        }
      }
      case "fuck" =>{
      }
      case Print() => {
        print()
      }
      case RouteFollowerList(uId, ctx) => {
        println("f")
        var plentyOfjsonmsgs = Jsonmsg.jsonmsgs
        if(containUserByUserId(uId)) {
          var flist = getUserByUserId(uId).getFollowerList()
          //sendBackToClient(flist, client)
          for(u <- flist ) {
            val newJsonmsg = Jfollower(u)
            plentyOfjsonmsgs = newJsonmsg :: plentyOfjsonmsgs
          }
          ctx.complete(Jsonmsg.toJson(plentyOfjsonmsgs))
        }
        else{
          var server = findServer(uId)
          server ! RouteFollowerList(uId, ctx)
        }
      }
      case RouteTimeLine(uId, ctx) => {
        println("t")
        var plentyOfjsonmsgs = Jsonmsg.jsonmsgs
        if(containUserByUserId(uId)) {
          var list = for(tid <- getUserByUserId(uId).getTimeLine) yield tweetMap.get(tid).get
          //sendBackToClient(list, ctx)
          for(u <- list ) {
            val newJsonmsg = Jtweet(u)
            plentyOfjsonmsgs = newJsonmsg :: plentyOfjsonmsgs
          }
          ctx.complete(Jsonmsg.toJson(plentyOfjsonmsgs))
        }
        else{
          var server = findServer(uId)
          server ! RouteTimeLine(uId, ctx)
        }
      }

      case RouteSentList(uId, ctx) => {
        println("s")

        if(containUserByUserId(uId)) {
          var plentyOfjsonmsgs = Jsonmsg.jsonmsgs
          var list = for(tid <- getUserByUserId(uId).getSentList()) yield tweetMap.get(tid).get
          //sendBackToClient(list, client)
          for(u <- list ) {
            val newJsonmsg = Jsent(u)
            plentyOfjsonmsgs = newJsonmsg :: plentyOfjsonmsgs
          }
          println("re")
          ctx.complete(Jsonmsg.toJson(plentyOfjsonmsgs))
        }
        else{
          var server = findServer(uId)
          server ! RouteSentList(uId, ctx)
        }
      }

      case Deliver(u, tId) => {
        println("Deliver "+u+ " "+tId)
        if(containUserByUserId(u)) {
          addToTweetList(u, tId)
        } else {
          var server = findServer(u)
          server ! Deliver(u, tId)
        }
      }

      case Update(uId, tId) => {
        var userPost = getUserByUserId(uId)
        userPost.addTweet(tId)
      }
    }
  }

  //  def main(args: Array[String]) : Unit = {
  //
  //  }
}
