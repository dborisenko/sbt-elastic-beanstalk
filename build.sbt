name := "sbt-elastic-beanstalk"
version := "0.4.10"
organization := "com.dbrsn"
organizationName := "OVO Energy"
scalaVersion := "2.12.3"
sbtPlugin := true

val awsJavaSdkVersion = "1.11.183"
val sbtNativePackagerVersion = "1.2.2"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % sbtNativePackagerVersion)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-elasticbeanstalk" % awsJavaSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsJavaSdkVersion
)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }
licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage := Some(url("https://github.com/dborisenko/sbt-elastic-beanstalk"))
scmInfo := Some(ScmInfo(url("https://github.com/dborisenko/sbt-elastic-beanstalk"), "scm:git:git://github.com:dborisenko/sbt-elastic-beanstalk.git"))

pomExtra :=
  <developers>
    <developer>
      <id>Ovo Energy</id>
      <name>Ovo Energy</name>
      <url>http://www.ovoenergy.com</url>
    </developer>
    <developer>
      <id>Denis Borisenko</id>
      <name>Denis Borisenko</name>
      <url>http://www.dbrsn.com</url>
    </developer>
  </developers>

