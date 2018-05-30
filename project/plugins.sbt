// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.15")

addSbtPlugin("com.gilt.sbt" % "sbt-newrelic" % "0.2.4")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")
