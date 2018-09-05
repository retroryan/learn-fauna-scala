name := "learn-fauna-scala"

version := "0.1"

scalaVersion := "2.12.4"

//val faunaVersion = "2.5.3"
val faunaVersion = "2.6.0-SNAPSHOT"


lazy val root = project.in(file("."))
  .settings(common)
  .settings(
    javaOptions in run += "-XX:-OmitStackTraceInFastThrow",
    libraryDependencies ++= Seq(

      //This uses an early release of the fauna jvm driver - 2.6.0-SNAPSHOT
      //drop a version of that jar file into a lib directory on the project root.
      //"com.faunadb" % "faunadb-scala_2.12" % faunaVersion,

      //jackson fasterxml is a transitive dependency of faunadb, so these are only
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.6",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.9.6",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.6",
    "com.codahale.metrics" % "metrics-core" % "3.0.2",
      "org.asynchttpclient" % "async-http-client" % "2.5.3",

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