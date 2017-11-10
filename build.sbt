import com.typesafe.config.ConfigFactory

/**
  * APP CONFIG
  */
lazy val applicationConfig = settingKey[Map[String, String]]("config values")

applicationConfig := {
  val conf = ConfigFactory.parseFile((resourceDirectory in Compile).value / "application.conf").resolve()
  val artifactoryConfig = conf.getConfig("env.default.artifactory")
  Map (
    "publishTrigger" -> artifactoryConfig.getBoolean("publish-init").toString,
    "artifactoryAddress" -> artifactoryConfig.getString("publish-repository"),
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
  val scala = "2.11.11"
  val scapegoatVersion = "1.1.0"
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
    art.copy(`type` = "jar", `classifier` = Some("assembly"))
  },
  // @TODO - add naming convention config in (Compile, assembly)
  artifactName := { (sv: ScalaVersion, module: ModuleID, artefact: Artifact) =>
    module.organization + "_" + artefact.name + "-" + artefact.classifier.getOrElse("package") + "-" + module.revision + "." + artefact.extension
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
    BuildInfoKey.action("repositoryAddress"){ Some(scmInfo.value.get.browseUrl).getOrElse("REPO_ADDRESS_NOT_FOUND")}
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
    case PathList("org", "slf4j", xs @ _*)                             => MergeStrategy.first
    case "application.conf"                                            => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  mainClass in assembly := Some("play.core.server.ProdServerStart"),
  fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
)

lazy val devDeps = Seq(
  ws,
  filters,
  "org.webjars"                  %%    "webjars-play"        %    "2.5.0-3",
  "com.typesafe.scala-logging"   %%    "scala-logging"       %    "3.5.0",
  "org.scalatestplus.play"       %%    "scalatestplus-play"  %    "2.0.0"           % Test,
  "io.swagger"                   %%    "swagger-play2"       %    "1.5.3",
  "io.lemonlabs"                 %%    "scala-uri"           %    "0.5.0",
  "org.webjars"                  %     "swagger-ui"          %    "2.2.10-1",
  "com.typesafe"                 %      "config"             %    "1.3.1"
    excludeAll ExclusionRule("commons-logging", "commons-logging")
)

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
    "-Xlint", // recommended additional warnings
    "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    //"-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures
    "-Ywarn-dead-code", // Warn when dead code is identified
    "-Ywarn-unused", // Warn when local and private vals, vars, defs, and types are unused
    "-Ywarn-unused-import", //  Warn when imports are unused (don't want IntelliJ to do it automatically)
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
    developers := List(Developer("Adrian Harris (Tech Lead)", "SBR", "ons-sbr-team@ons.gov.uk", new java.net.URL(s"https:///v1/home"))),
    moduleName := "sbr-api",
    organizationName := "ons",
    description := "<description>",
    version := (version in ThisBuild).value,
    name := s"${organizationName.value}-${moduleName.value}",
    licenses := Seq("MIT-License" -> url("https://github.com/ONSdigital/sbr-control-api/blob/master/LICENSE")),
    startYear := Some(2017),
    homepage := Some(url("https://SBR-UI-HOMEPAGE.gov.uk")),
    libraryDependencies ++= devDeps
  )
