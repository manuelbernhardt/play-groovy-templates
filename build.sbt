name := "groovy-template-engine"

organization := "play"

resolvers += "jahia" at "http://maven.jahia.org/maven2"

libraryDependencies ++= Seq(
	"org.codehaus.groovy" % "groovy" % "1.8.5",
    "commons-collections" % "commons-collections" % "3.2.1",
	"commons-lang" % "commons-lang" % "2.6",
	"com.jamonapi" % "jamon" % "2.7")
