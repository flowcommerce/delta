name := "delta"

organization := "io.flow"

scalaVersion in ThisBuild := "2.13.3"

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtWeb)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      ws,
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.7.1" cross CrossVersion.full),
      "com.github.ghik" %% "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
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
      "org.yaml" % "snakeyaml" % "1.27"
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
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.7",
    routesImport += "io.flow.delta.v0.Bindables.Core._",
    routesImport += "io.flow.delta.v0.Bindables.Models._",
    routesImport += "io.flow.delta.config.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    testOptions += Tests.Argument("-oF"),
    libraryDependencies ++= Seq(
      jdbc,
      "com.amazonaws" % "aws-java-sdk-ec2" % "1.11.925",
      "com.amazonaws" % "aws-java-sdk-ecs" % "1.11.925",
      "com.amazonaws" % "aws-java-sdk-ecr" % "1.11.925",
      "com.amazonaws" % "aws-java-sdk-elasticloadbalancing" % "1.11.925",
      "com.amazonaws" % "aws-java-sdk-autoscaling" % "1.11.925",
      "com.amazonaws" % "aws-java-sdk-sns" % "1.11.925",
      "com.typesafe.play" %% "play-json-joda" % "2.9.1",
      "org.postgresql" % "postgresql" % "42.2.18",
      "com.sendgrid" %  "sendgrid-java" % "4.7.1",
      "io.flow" %% "lib-akka-akka26" % "0.1.39",
      "io.flow" %% "lib-event-play28" % "1.0.64",
      "io.flow" %% "lib-log" % "0.1.28",
      "io.flow" %% "lib-postgresql-play-play28" % "0.4.1",
      "io.flow" %% "lib-play-graphite-play28" % "0.1.75",
      "io.flow" %% "lib-usage-play28" % "0.1.44",
      "io.kubernetes" % "client-java" % "5.0.0",
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.7.1" cross CrossVersion.full),
      "com.github.ghik" %% "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
    ),
    // silence all warnings on autogenerated files
    flowGeneratedFiles ++= Seq(
      "target/*".r,
      "app/generated/.*".r,
      "app/db/generated/.*".r,
    ),
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
    javaOptions in Test += "-Dconfig.file=conf/test.conf",
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
    routesImport += "io.flow.delta.config.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    testOptions += Tests.Argument("-oF"),
    libraryDependencies ++= Seq(
      "org.webjars" % "bootstrap" % "3.4.1",
      "org.webjars.bower" % "bootstrap-social" % "5.1.1",
      "org.webjars" %% "webjars-play" % "2.8.0",
      "org.webjars" % "font-awesome" % "5.15.1",
      "org.webjars" % "jquery" % "3.5.1",
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.7.1" cross CrossVersion.full),
      "com.github.ghik" %% "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
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
    "io.flow" %% "lib-play-play28" % "0.6.25",
    "io.flow" %% "lib-test-utils-play28" % "0.1.15" % Test,
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
version := "0.8.84"
