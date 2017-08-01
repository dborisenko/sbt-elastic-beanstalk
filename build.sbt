name := "sbt-elastic-beanstalk"
version := "0.3.2"
organization := "com.dbrsn"
organizationName := "OVO Energy"
scalaVersion := "2.10.6"
sbtPlugin := true

val awsJavaSdkVersion = "1.11.171"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-elasticbeanstalk" % awsJavaSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsJavaSdkVersion
)

licenses := Seq("MIT License" -> url("https://github.com/dborisenko/sbt-elastic-beanstalk/blob/master/LICENSE"))
homepage := Some(url("https://github.com/dborisenko/sbt-elastic-beanstalk"))
scmInfo := Some(ScmInfo(url("https://github.com/dborisenko/sbt-elastic-beanstalk"), "scm:git:git://github.com:dborisenko/sbt-elastic-beanstalk.git"))


publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

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

