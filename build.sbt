name := "learn-fauna-scala"

version := "0.1"

scalaVersion := "2.12.4"

//val faunaVersion = "2.5.3"
val faunaVersion = "2.6.0-SNAPSHOT"


lazy val root = project.in(file("."))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
    "com.faunadb" % "faunadb-scala_2.12" % faunaVersion,
    "com.typesafe.akka" %% "akka-actor" % "2.5.3",
    "com.typesafe" % "config" % "1.3.1",
    "io.spray" %%  "spray-json" % "1.3.3",
    "com.github.nscala-time" %% "nscala-time" % "2.18.0",
    "com.iheart" %% "ficus" % "1.4.3",
    // These are here to support logging and get rid of the ugly SLF4J error messages
    "org.slf4j" % "slf4j-api" % "1.7.5",
    "org.slf4j" % "slf4j-simple" % "1.7.5",
    "org.clapper" %% "grizzled-slf4j" % "1.3.2"
   ),
    trapExit := false
  )


lazy val common =
  Seq(
    scalaVersion := "2.12.4",
    organization := "com.fauna",
    version := "0.1",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Xlint",
      "-Ypartial-unification",
      "-target:jvm-1.8",
      "-encoding", "UTF-8"
    )
  )