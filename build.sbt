val Version = "1.0"

val settings = Seq(
  organization := "com.igorvovk",
  version := Version,
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature"),
  publishArtifact in Test := false,
  libraryDependencies ++= {
    val playVer = "2.5.0"
    val akkaVer = "2.4.2"

    Seq(
      "com.typesafe.play" %% "play-json" % playVer,
      "com.typesafe.play" %% "play-ws" % playVer,
      "com.typesafe.play" %% "play-logback" % playVer,
      "com.typesafe.akka" %% "akka-actor" % akkaVer,
      "com.typesafe.akka" %% "akka-persistence" % akkaVer,
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    )
  }
)


val publishSettings = {
  if (Version.endsWith("-SNAPSHOT")) {
    Seq(
      bintrayReleaseOnPublish := false
    )
  } else {
    Seq(
      publishArtifact in Test := false,
      licenses := ("MIT", url("http://opensource.org/licenses/MIT")) :: Nil
    )
  }
}

lazy val root: Project = Project(
  "telegram-bot-api",
  file("."),
  settings = settings ++ publishSettings
)
