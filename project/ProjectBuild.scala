import sbt._
import Keys._

object ProjectBuild extends Build {

  val buildVersion = "0.8.0"

  val releases = "Sonatype OSS Releases Repository" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  val snapshots = "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots"
  val publicationRepository = if(buildVersion.endsWith("SNAPSHOT")) snapshots else releases

  lazy val root = Project(
    id = "groovy-templates-engine",
    base = file(".")
  ).settings(

    organization := "io.bernhardt",

    version := buildVersion,

    resolvers += "jahia" at "http://maven.jahia.org/maven2",
    resolvers += "codehaus" at "http://repository.codehaus.org/",

    libraryDependencies ++= Seq(
      "org.codehaus.groovy"       % "groovy-all"          % "2.0.0",
      "commons-collections"       % "commons-collections" % "3.2.1",
      "commons-lang"              % "commons-lang"        % "2.6"
    ),

    publishTo := Some(publicationRepository),

    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

    publishMavenStyle := true,

    crossPaths := false

  )

}
