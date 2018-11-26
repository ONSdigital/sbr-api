import com.typesafe.config.ConfigFactory
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerBaseImage

/**
  * APP CONFIG
  */
lazy val applicationConfig = settingKey[Map[String, String]]("config values")

applicationConfig := {
  val conf = ConfigFactory.parseFile((resourceDirectory in Compile).value / "application.conf").resolve()
  val artifactoryConfig = conf.getConfig("artifactory")
  Map (
    "publishTrigger" -> artifactoryConfig.getBoolean("publish.init").toString,
    "artifactoryAddress" -> artifactoryConfig.getString("publish.repository"),
    "artifactoryHost" -> artifactoryConfig.getString("host"),
    "artifactoryUser" -> artifactoryConfig.getString("user"),
    "artifactoryPassword" -> artifactoryConfig.getString("password")
  )
}


/**
  * KEY-BINDING(S)
  */
lazy val ITest = config("it") extend Test


/**
  * HARD VARS
  */
lazy val Versions = new {
  val scala = "2.12.7"
}

lazy val Constant = new {
  val projectStage = "alpha"
  val team = "sbr"
  val local = "mac"
}


/**
  * SETTINGS AND CONFIGURATION
  */
lazy val Resolvers: Seq[MavenRepository] = Seq(
  Resolver.typesafeRepo("releases")
)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  sourceDirectory in ITest := baseDirectory.value / "test/it",
  resourceDirectory in ITest := baseDirectory.value / "test/resources",
  scalaSource in ITest := baseDirectory.value / "test/it",
  // test setup
  parallelExecution in Test := false
)

lazy val noPublishSettings: Seq[Def.Setting[_]] = Seq(
  publish := {},
  publishLocal := {}
)

lazy val publishingSettings: Seq[Def.Setting[_]] = Seq(
  publishArtifact := applicationConfig.value("publishTrigger").toBoolean,
  publishMavenStyle := false,
  checksums in publish := Nil,
  publishArtifact in Test := false,
  publishArtifact in (Compile, packageBin) := false,
  publishArtifact in (Compile, packageSrc) := false,
  publishArtifact in (Compile, packageDoc) := false,
  publishTo := {
    if (System.getProperty("os.name").toLowerCase.startsWith(Constant.local) )
      Some(Resolver.file("file", new File(s"${System.getProperty("user.home").toLowerCase}/Desktop/")))
    else
      Some("Artifactory Realm" at applicationConfig.value("artifactoryAddress"))
  },
  artifact in (Compile, assembly) ~= { art =>
    art.withType("jar").withClassifier(Some("assembly"))
  },
  // @TODO - add naming convention config in (Compile, assembly)
  artifactName := { (_: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    module.organization + "_" + artifact.name + "-" + artifact.classifier.getOrElse("package") + "-" + module.revision + "." + artifact.extension
  },
  credentials += Credentials("Artifactory Realm", applicationConfig.value("artifactoryHost"),
    applicationConfig.value("artifactoryUser"), applicationConfig.value("artifactoryPassword")),
  releaseTagComment := s"Releasing $name ${(version in ThisBuild).value}",
  releaseCommitMessage := s"Setting Release tag to ${(version in ThisBuild).value}",
  // no commit - ignore zip and other package files [Jenkins]
  releaseIgnoreUntrackedFiles := true
)

lazy val buildInfoConfig: Seq[Def.Setting[_]] = Seq(
  buildInfoPackage := "controllers",
  // gives us last compile time and tagging info
  buildInfoKeys := Seq[BuildInfoKey](
    organizationName,
    moduleName,
    name,
    description,
    developers,
    version,
    scalaVersion,
    sbtVersion,
    startYear,
    homepage,
    BuildInfoKey.action("gitVersion") {
      git.formattedShaVersion.?.value.getOrElse(Some("Unknown")).getOrElse("Unknown") +"@"+ git.formattedDateVersion.?.value.getOrElse("")
    },
    BuildInfoKey.action("codeLicenses"){ licenses.value },
    BuildInfoKey.action("projectTeam"){ Constant.team },
    BuildInfoKey.action("projectStage"){ Constant.projectStage },
    BuildInfoKey.action("repositoryAddress"){ scmInfo.value.fold("REPO_ADDRESS_NOT_FOUND")(_.browseUrl.toExternalForm) }
  ),
  // di router -> swagger
  routesGenerator := InjectedRoutesGenerator,
  buildInfoOptions += BuildInfoOption.ToMap,
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.BuildTime
)

lazy val assemblySettings: Seq[Def.Setting[_]] = Seq(
  assemblyJarName in assembly := s"${organizationName.value}-${moduleName.value}-assembly-${version.value}.jar",
  assemblyMergeStrategy in assembly := {
    case PathList("javax", "servlet", xs @ _*)                         => MergeStrategy.last
    case PathList("org", "apache", xs @ _*)                            => MergeStrategy.last
    case PathList("org", "slf4j", xs @ _*)                             => MergeStrategy.first
    case PathList("META-INF", "io.netty.versions.properties", xs @ _*) => MergeStrategy.last
    case "application.conf"                                            => MergeStrategy.first
    case "logback.xml"                                                 => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  mainClass in assembly := Some("play.core.server.ProdServerStart"),
  fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
)

lazy val deps = Seq(
  ws,
  filters,
  guice,
  "org.scalatestplus.play"       %%    "scalatestplus-play"          % "3.1.2"   % Test,
  "org.scalatest"                %%    "scalatest"                   % "3.0.5"   % Test,
  "com.github.tomakehurst"       %     "wiremock"                    % "2.19.0"  % Test,
  "org.scalamock"                %%    "scalamock"                   % "4.1.0"   % Test,
  "com.typesafe.scala-logging"   %%    "scala-logging"               % "3.9.0",
  "io.swagger"                   %%    "swagger-play2"               % "1.6.0",
  "io.lemonlabs"                 %%    "scala-uri"                   % "1.4.0",
  "org.webjars"                  %     "swagger-ui"                  % "3.19.5",
  "com.typesafe"                 %     "config"                      % "1.3.3"
    excludeAll ExclusionRule("commons-logging", "commons-logging"),
  "jp.co.bizreach"               %%    "play-zipkin-tracing-play26"  % "1.4.0"
)

lazy val depOverrides = Seq(
  "org.scala-lang.modules"       %%    "scala-parser-combinators"    % "1.1.0",
  "org.reactivestreams"          %     "reactive-streams"            % "1.0.2",
  "com.google.code.findbugs"     %     "jsr305"                      % "3.0.2",
  "org.apache.commons"           %     "commons-lang3"               % "3.6",
  "org.scalatest"                %%    "scalatest"                   % "3.0.5",
  "com.google.guava"             %     "guava"                       % "22.0",
  "com.typesafe.play"            %%    "play-test"                   % "2.6.20",
  "com.typesafe.play"            %%    "play-ws"                     % "2.6.20",
  "com.typesafe.play"            %%    "play-ahc-ws"                 % "2.6.20",

  // wiremock requires jetty 9.2.24.v20180105 but play-test's selenium dependency is transitively pulling in a binary incompatible 9.4.5.v20170502
  "org.eclipse.jetty"            %     "jetty-http"                  % "9.2.24.v20180105",
  "org.eclipse.jetty"            %     "jetty-io"                    % "9.2.24.v20180105",
  "org.eclipse.jetty"            %     "jetty-util"                  % "9.2.24.v20180105",
  "com.fasterxml.jackson.core"   %     "jackson-databind"            % "2.8.11.2",
  "org.apache.httpcomponents"    %     "httpclient"                  % "4.5.5",

  // conflicts resulting from io.swagger:swagger-play2 (treat swagger as low priority and select latest versions)
  "com.typesafe.play"            %%    "twirl-api"                   % "1.3.15",
  "com.typesafe.play"            %%    "play-server"                 % "2.6.20",
  "com.typesafe.play"            %%    "filters-helpers"             % "2.6.20",
  "com.typesafe.play"            %%    "play-logback"                % "2.6.20",
  "com.typesafe.play"            %%    "play-akka-http-server"       % "2.6.20",
  "org.slf4j"                    %     "slf4j-api"                   % "1.7.25"
)

/*
 * -Ywarn-unused-import was removed because otherwise a large number of warnings are generated for
 *                      sbr-api/conf/routes which is a Play issue we can do nothing about
 * In the upgrade to Scala 2.12 we have unfortunately had to disable unused warnings completely with
 * -Xlint:-unused (i.e enable Xlint except for unused).
 * This is because in addition to some Play requirements that were being flagged as unused, private
 * implicit vals were being flagged as unused even when used as a result of implicit resolution.
 */
lazy val commonSettings: Seq[Def.Setting[_]] = Seq (
  scalacOptions in ThisBuild ++= Seq(
    "-language:experimental.macros",
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-language:reflectiveCalls",
    "-language:experimental.macros",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:postfixOps",
    "-deprecation", // warning and location for usages of deprecated APIs
    "-feature", // warning and location for usages of features that should be imported explicitly
    "-unchecked", // additional warnings where generated code depends on assumptions
    "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
    "-Xlint:-unused", // recommended additional warnings
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures
    "-Ywarn-dead-code", // Warn when dead code is identified
    "-Ywarn-numeric-widen" // Warn when numerics are widened
  ),
  resolvers ++= Resolvers,
  coverageExcludedPackages := ".*Routes.*;.*ReverseRoutes.*;.*javascript.*"
)


/**
  * PROJECT DEF
  */
lazy val api = (project in file("."))
  .enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt, PlayScala)
  .configs(ITest)
  .settings(inConfig(ITest)(Defaults.testSettings) : _*)
  .settings(commonSettings: _*)
  .settings(testSettings:_*)
  .settings(publishingSettings:_*)
//  .settings(noPublishSettings:_*)
  .settings(buildInfoConfig:_*)
  // assembly
  .settings(assemblySettings:_*)
  .settings(
    scalaVersion := Versions.scala,
    scapegoatVersion in ThisBuild := "1.3.8",
    developers := List(Developer("Adrian Harris (Tech Lead)", "SBR", "ons-sbr-team@ons.gov.uk", new java.net.URL(s"https:///v1/home"))),
    moduleName := "sbr-api",
    organizationName := "ons",
    description := "<description>",
    version := (version in ThisBuild).value,
    name := s"${organizationName.value}-${moduleName.value}",
    licenses := Seq("MIT-License" -> url("https://github.com/ONSdigital/sbr-control-api/blob/master/LICENSE")),
    startYear := Some(2017),
    homepage := Some(url("https://SBR-UI-HOMEPAGE.gov.uk")),
    conflictManager := ConflictManager.strict,
    libraryDependencies ++= deps,
    dependencyOverrides ++= depOverrides,
    dockerBaseImage := "openjdk:8-jre",
    dockerExposedPorts := Seq(9000)
  )
