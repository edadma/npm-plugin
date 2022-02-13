name := "npm-plugin"

version := "0.1.14"

scalaVersion := "2.12.15" // don't change (sbt 1.x uses Scala 2.12.x)

sbtPlugin := true

enablePlugins(SbtPlugin)

scalacOptions ++= Seq( "-deprecation", "-feature", "-unchecked", "-language:postfixOps", "-language:implicitConversions", "-language:existentials" )

organization := "io.github.edadma"

githubOwner := "edadma"

githubRepository := "npm-plugin"

Global / onChangedBuildSource := ReloadOnSourceChanges

resolvers += "Hyperreal Repository" at "https://dl.bintray.com/edadma/maven"

libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-json" % "2.8.1"
)

mainClass := Some("Main")

publishMavenStyle := true

Test / publishArtifact := false

licenses := Seq("ISC" -> url("https://opensource.org/licenses/ISC"))
