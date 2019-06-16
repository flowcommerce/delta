// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")

addSbtPlugin("com.gilt.sbt" % "sbt-newrelic" % "0.3.1")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.4")

resolvers += "Flow Plugins" at "https://flow.jfrog.io/flow/plugins-release/"
addSbtPlugin("io.flow" % "sbt-flow-linter" % "0.0.6")
