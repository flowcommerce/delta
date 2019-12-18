import play.sbt.PlayScala._

name := "delta"

organization := "io.flow"

scalaVersion in ThisBuild := "2.12.10"

val awsVersion = "1.11.693"

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtWeb)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      ws,
      "com.github.ghik" %% "silencer-lib" % "1.3.0" % Provided,
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.3.0"),
    ),
    // silence all warnings on autogenerated files
    flowGeneratedFiles ++= Seq(
      "app/.*".r,
    ),
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
  )

lazy val lib = project
  .in(file("lib"))
  .dependsOn(generated)
  .aggregate(generated)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.yaml" % "snakeyaml" % "1.25"
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .enablePlugins(PlayScala)
  .enablePlugins(NewRelic)
  .enablePlugins(JavaAppPackaging, JavaAgent)
  .settings(commonSettings: _*)
  .settings(
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.4",
    routesImport += "io.flow.delta.v0.Bindables.Core._",
    routesImport += "io.flow.delta.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    testOptions += Tests.Argument("-oF"),
    libraryDependencies ++= Seq(
      jdbc,
      "io.flow" %% "lib-postgresql-play-play26" % "0.3.52",
      "io.flow" %% "lib-event-play26" % "1.0.21",
      "com.amazonaws" % "aws-java-sdk-ec2" % awsVersion,
      "com.amazonaws" % "aws-java-sdk-ecs" % awsVersion,
      "com.amazonaws" % "aws-java-sdk-ecr" % awsVersion,
      "com.amazonaws" % "aws-java-sdk-elasticloadbalancing" % awsVersion,
      "com.amazonaws" % "aws-java-sdk-autoscaling" % awsVersion,
      "com.amazonaws" % "aws-java-sdk-sns" % awsVersion,
      "com.sendgrid" %  "sendgrid-java" % "4.4.1",
      "org.postgresql" % "postgresql" % "42.2.8",
      "com.typesafe.play" %% "play-json-joda" % "2.7.4",
      "io.flow" %% "lib-play-graphite-play26" % "0.1.26",
      "io.flow" %% "lib-log" % "0.0.91",
      "io.flow" %% "lib-akka" % "0.1.13",
      "io.flow" %% "lib-usage" % "0.1.9",
      "io.kubernetes" % "client-java" % "5.0.0",
      "com.github.ghik" %% "silencer-lib" % "1.3.0" % Provided,
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.3.0"),
    ),
    // silence all warnings on autogenerated files
    flowGeneratedFiles ++= Seq(
      "target/*".r,
      "app/generated/.*".r,
      "app/db/generated/.*".r,
    ),
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
  )

lazy val www = project
  .in(file("www"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .enablePlugins(PlayScala)
  .enablePlugins(NewRelic)
  .enablePlugins(SbtWeb)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "io.flow.delta.v0.Bindables.Core._",
    routesImport += "io.flow.delta.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    testOptions += Tests.Argument("-oF"),
    libraryDependencies ++= Seq(
      "org.webjars" %% "webjars-play" % "2.6.3",
      "org.webjars" % "bootstrap" % "3.4.1",
      "org.webjars.bower" % "bootstrap-social" % "5.1.1",
      "org.webjars" % "font-awesome" % "5.11.2",
      "org.webjars" % "jquery" % "2.1.4",
      "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided,
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.4.2"),
    ),
    // silence all warnings on autogenerated files
    flowGeneratedFiles ++= Seq(
      "target/*".r,
    ),
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
  )

val credsToUse = Option(System.getenv("ARTIFACTORY_USERNAME")) match {
  case None => Credentials(Path.userHome / ".ivy2" / ".artifactory")
  case _ => Credentials("Artifactory Realm","flow.jfrog.io",System.getenv("ARTIFACTORY_USERNAME"),System.getenv("ARTIFACTORY_PASSWORD"))
}


lazy val commonSettings: Seq[Setting[_]] = Seq(
  name ~= ("delta-" + _),
  libraryDependencies ++= Seq(
    ws,
    guice,
    "io.flow" %% "lib-play-play26" % "0.5.84",
    "io.flow" %% "lib-test-utils" % "0.0.73" % Test,
  ),
  sources in (Compile,doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false,
  scalacOptions += "-feature",
  resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "Artifactory" at "https://flow.jfrog.io/flow/libs-release/",
  credentials += Credentials(
    "Artifactory Realm",
    "flow.jfrog.io",
    System.getenv("ARTIFACTORY_USERNAME"),
    System.getenv("ARTIFACTORY_PASSWORD")
  )
)
version := "0.7.94"
