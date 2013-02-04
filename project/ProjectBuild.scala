import sbt._
import Keys._

object ProjectBuild extends Build {

  val buildVersion = "0.7.5"

  val delvingReleases = "Delving Releases Repository" at "http://development.delving.org:8081/nexus/content/repositories/releases"
  val delvingSnapshots = "Delving Snapshot Repository" at "http://development.delving.org:8081/nexus/content/repositories/snapshots"
  val delvingRepository = if (buildVersion.endsWith("SNAPSHOT")) delvingSnapshots else delvingReleases

  lazy val root = Project(
    id = "groovy-templates-engine",
    base = file(".")
  ).settings(

    organization := "eu.delving",

    version := buildVersion,

    resolvers += "jahia" at "http://maven.jahia.org/maven2",
    resolvers += "codehaus" at "http://repository.codehaus.org/",

    libraryDependencies ++= Seq(
      "org.codehaus.groovy"       % "groovy-all"          % "2.0.0",
      "commons-collections"       % "commons-collections" % "3.2.1",
      "commons-lang"              % "commons-lang"        % "2.6",
      "com.jamonapi"              % "jamon"               % "2.7"
    ),

    publishTo := Some(delvingRepository),

    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

    publishMavenStyle := true,

    crossPaths := false

  )

}
