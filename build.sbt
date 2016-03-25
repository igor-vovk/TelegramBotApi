name := """telegram-bot-api"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val playVer = "2.5.0"
  val akkaVer = "2.4.2"

  Seq(
    "com.typesafe.play" %% "play-json" % playVer,
    "com.typesafe.play" %% "play-ws" % playVer,
    "com.typesafe.akka" %% "akka-actor" % akkaVer,
    "com.typesafe.akka" %% "akka-persistence" % akkaVer % "provided",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )
}

