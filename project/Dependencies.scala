import sbt._

object Dependencies {

  object Versions {
    val scala = "2.12.6"

    val `aws-java-sdk` = "1.11.323"
    val `sbt-native-packager` = "1.3.4"
  }

  val `sbt-native-packager` = "com.typesafe.sbt" % "sbt-native-packager" % Versions.`sbt-native-packager`
  val `aws-java-sdk-elasticbeanstalk` = "com.amazonaws" % "aws-java-sdk-elasticbeanstalk" % Versions.`aws-java-sdk`
  val `aws-java-sdk-s3` = "com.amazonaws" % "aws-java-sdk-s3" % Versions.`aws-java-sdk`
}
