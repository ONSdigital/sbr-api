import play.sbt.PlayScala
//import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin.autoImport._


licenses := Seq("MIT-License" -> url("https://opensource.org/licenses/MIT"))

lazy val versions = new {
  val scala = "2.11.11"
  val version = "0.1"
  val scapegoatVersion = "1.1.0"
  val util = "0.27.8"
}


lazy val constant = new {
  val appName = "ons-sbr-api"
  val detail = versions.version
  val organisation = "ons"
  val team = "sbr"
}

lazy val commonSettings = Seq (
  scalaVersion := versions.scala,
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
  resolvers ++= Seq(
    Resolver.typesafeRepo("releases"),
    Resolver.bintrayRepo("outworkers", "oss-releases")
  ),
  coverageExcludedPackages := ".*Routes.*;.*ReverseRoutes.*;.*javascript.*"
)



lazy val api = (project in file("."))
  .enablePlugins(BuildInfoPlugin, GitVersioning, PlayScala)
  .settings(commonSettings: _*)
  .settings(
    scalaVersion := versions.scala,
    name := constant.appName,
    moduleName := "ons-sbr-api",
    version := versions.version,
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      name,
      version,
      BuildInfoKey.action("gitVersion") {
      git.formattedShaVersion.?.value.getOrElse(Some("Unknown")).getOrElse("Unknown") +"@"+ git.formattedDateVersion.?.value.getOrElse("")
    }),
    buildInfoPackage := "controllers",
        libraryDependencies ++= Seq (
          filters,
          "com.outworkers" %% "util-parsers-cats" % versions.util,
          "com.outworkers" %% "util-play" % versions.util,
          "com.outworkers" %% "util-testing" % versions.util % Test,
          "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
        )
  )



//scalaVersion := "2.11.11"
// += will append a single element to sequence
// ++= whereas, concat another seq
libraryDependencies += jdbc
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test