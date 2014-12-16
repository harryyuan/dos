package client

//package com.softwaremill.spray.Client

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.routing.BroadcastPool
import spray.client.pipelining._
//import spray.json.SprayJsonSupport._

import akka.actor.Props

import scala.concurrent.Lock
import scala.concurrent.duration.Duration
import scala.util.Random
import scala.util.control.Breaks._
/**
 * Created by whw on 2014/12/11.
 */
object Client extends App {

  case class StartTweet(sleeptime : Int)

  implicit val system = ActorSystem()

  import Client.system.dispatcher

  val pipeline = sendReceive

  var ip: String = args(0)
  //   var ip : String = "127.0.0.1"
  var startIdNum: Int = 0
  var partNum: Int = 0
  var router: ActorRef = null
  var lock: Lock = new Lock()
  var second = Duration.create(1, "millis")
  var tenMili = Duration.create(1, "millis")
  var userNum: Int = 0
  var port="8091"
  var sentList = new Array[String](5200)
  var sentListGot=0
  //  def generateRandomTweet: String = {
  //    (for (i <- 1 until Random.nextInt(141)) yield Random.nextPrintableChar()).mkString("")
  //  }


  def generateRandomTweet : String = {
    //      (for(i <- 1 until Random.nextInt(141)) yield Random.nextPrintableChar()).mkString("")
    (for(i <- 1 until Random.nextInt(20)) yield Random.nextInt()).mkString("")
  }


  def printlines = {
    println("please type in command:")
    println("1:StartTweet (Duration ms actorNum)")
    println("2:PostTweet (uId tweet)")
    println("3:GetSentList (uId)")
    println("4:GetTimeLine (uId)")
    println("5:GetFollowerList (uId)")
    println("6:GetTweetNum")
    println("7:follow (uId fId)")
    println("8:DeleteTweet")
    println("9:stop")
    println();
  }

  breakable {
    printlines

    while (true) {




      val ln = readLine();

      val arguments = ln.split(" ")

     if(arguments(0) =="help"){
        printlines
      }else
      if (arguments(0) == "1") {
        val clientSystem = ActorSystem("project")
        val router = clientSystem.actorOf(BroadcastPool(arguments(2).toInt).props(Props[ClientInit]), "router")
        var sleeptime=arguments(1).toInt
        router ! StartTweet(sleeptime)
        printlines
      }

      else if (arguments(0) == "2") {
        var uId = arguments(1)
        //        userNum = args(1).toInt
        //        var u = Random.nextInt(userNum).toString
        var tweet = arguments(2)
        pipeline(Post("http://"+ip+":"+port+"/posttweet/"+uId+"/"+tweet))
        printlines
      }

      else if (arguments(0) == "3") {
        var uId = arguments(1)
        //    val result = pipeline(Get("http://localhost:8080/getsentlist?uId="+uId))
        val result = pipeline(Get("http://"+ip+":"+port+"/getsentlist/" + uId))
        sentListGot=1

        var jsonString=""
        result.foreach { response =>
          println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
          var jsonString=response.entity.asString

          if(jsonString.length>6) {
            var jsonList = jsonString.substring(1, jsonString.length() - 1).split("},")
            var i = 0
            jsonList.foreach { uidTweet =>
              sentList(i) = uidTweet.split("\"")(7)

              i = i + 1
            }
          }
        }
        printlines
      }

      else if (arguments(0) == "4") {

        var uId = arguments(1)
        val result = pipeline(Get("http://"+ip+":"+port+"/gettimeline/" + uId))
        result.foreach { response =>
          println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
        }
        //    var server = context.actorSelection("akka.tcp://Server@"+
        //      ip+":8888/user/master")
        //    server ! GetTimeLine(uId)
        printlines
      }
      else if (arguments(0) == "5") {

        var uId = arguments(1)
        val result = pipeline(Get("http://"+ip+":"+port+"/getfollowerlist/" + uId))
        result.foreach { response =>
          println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
        }
        //    var server = context.actorSelection("akka.tcp://Server@"+
        //      ip+":8888/user/master")
        //    server ! GetFollowerList(uId : String)
        printlines
      }

      else if (arguments(0) == "TweetNum") {
        var num = arguments(1)
        println(num)
        printlines
      }

      else if (arguments(0) == "6") {
        val result = pipeline(Get("http://"+ip+":"+port+"/gettweetnum"))
        result.foreach { response =>
          println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
          printlines
        }
        //    var server = context.actorSelection("akka.tcp://Server@"+
        //      ip+":8888/user/master")
        //    server ! GetTweetNum
      }

      //  else if(arguments(0) == "SendBack"){
      //
      //  }
      //
      //  else if(arguments(0) == "SendBackFollower"){
      //
      //  }


      else if (arguments(0) == "7") {


        var uid = arguments(1)
        var fid = arguments(2)
        //    pipeline(Post("http://localhost:8080/follow?uId="+uid+"&fId="+fid))
        pipeline(Post("http://"+ip+":"+port+"/follow/" + uid + "/" + fid))
        //    var server = context.actorSelection("akka.tcp://Server@"+
        //      ip+":8888/user/master")
        //    server ! Follow(uId, fId)
        printlines
      }

      else if (arguments(0) == "8") {

        if(sentListGot == 0){
          println("please getsentlist first")
          println()
          printlines
        }
        else {

          var j = 0
          sentList.foreach { sentTweet =>
            if (sentTweet != null) {
            println(j + " " + sentTweet)
            j = j + 1
            }
          }
          println("please select which one to delete")
          val chose = readLine();
          var chosenTweetTuple=sentList(chose.toInt)
          var uidandtweet =chosenTweetTuple.split(":")
          var uid = uidandtweet(0)
          var tweet = uidandtweet(1).substring(1)
          var tid = tweet.hashCode().toString
          pipeline(Post("http://"+ip+":"+port+"/deletetweet/" + uid + "/" + tid))
          printlines
        }


      }

      //  else {
      //    println("miss print")
      //  }
      else if (arguments(0) == "stop") {
        break
        //          println("111")
      }
    }
  }



  class ClientInit extends Actor{



    def receive = {
      case StartTweet(sleeptime) => {
        import context.dispatcher
        //        var server = context.actorSelection("akka.tcp://Server@"+
        //          ip+":8888/user/master")
        var sche = context.system.scheduler.schedule(second, tenMili) {
          userNum = args(1).toInt
          var u = Random.nextInt(userNum).toString
          var tweet = generateRandomTweet
          while(tweet==""){
            tweet = generateRandomTweet
          }
          var tId = tweet.hashCode().toString
          //          pipeline(Post("http://"+ip+":8080/starttweet?u="+u+"&tweet="+tweet+"&tid="+tId))

          //          var subs=tweet.split("/")
          //          var s = ""
          //          subs.foreach{ sub=>
          //            s = s+sub
          //          }
          //         var subs1=s.split("\"")
          //         var s1 = ""
          //         subs1.foreach{ sub1=>
          //         s1 = s1+sub1
          //         }
          //        var subs2=s1.split("%25")
          //        var s2 = ""
          //        subs1.foreach{ sub2=>
          //         s2 = s2+sub2
          //        }
          //
          //          tweet = s2

          //  pipeline(Post("http://"+ip+":8080/starttweet?u="+u+"&tweet="+tweet+"&tid="+tId))
          pipeline(Post("http://"+ip+":"+port+"/posttweet/"+u+"/"+tweet))
          //println("http://"+ip+":"+port+"/posttweet/"+u+"/"+tweet)

          //  println("http://"+ip+":8080/starttweet?u="+u+"&tweet="+tweet+"&tid="+tId)
          //         pipeline(Post("http://"+ip+":8080/starttweet", { "u"="whatever" }""".asJson))
        }
        Thread.sleep(sleeptime)
        sche.cancel()
        println("Done")
        //        println("please type in command:")
        //        print("1:StartTweet")
        //        print("  2:PostTweet")
        //        print("  3:GetSentList")
        //        println("  4:GetTimeLine")
        //        print("5:GetFollowerList")
        //        print("  6:GetTweetNum")
        //        print("  7:follow")
        //        print("  8:stop")
        //        println();
      }


    }
  }


  //  val securePipeline = addCredentials(BasicHttpCredentials("adam", "1234")) ~> sendReceive
  //
  //  val result = securePipeline(Get("http://localhost:8080/list/all"))
  //  result.foreach { response =>
  //    println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
  //  }
  //
  //  pipeline(Post("http://localhost:8080/amber/add/mined?country=Estonia&size=10"))
  //
  //  Thread.sleep(1000L)
  //
  //  system.shutdown()
  //  system.awaitTermination()



}
