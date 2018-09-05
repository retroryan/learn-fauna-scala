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

      //These are all transitive dependencies of faunadb, so they are only needed when using the fauna jar in the lib directory
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.6",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.9.6",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.6",
      "com.codahale.metrics" % "metrics-core" % "3.0.2",
      "org.asynchttpclient" % "async-http-client" % "2.5.3",


      //These are the required dependencies
      "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.9.0",
      "com.typesafe" % "config" % "1.3.1",
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