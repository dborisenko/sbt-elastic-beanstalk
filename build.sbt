import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.GitHubHosting
import xerial.sbt.Sonatype.SonatypeCommand.sonatypeRelease

inThisBuild(List(
  organization := "com.dbrsn",
  scalaVersion := Dependencies.Versions.scala,
  scalacOptions := Seq(
    "-deprecation", // Warning and location for usages of deprecated APIs
    "-encoding", "UTF-8",
    "-feature", // Warning and location for usages of features that should be imported explicitly
    "-unchecked", // Additional warnings where generated code depends on assumptions
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xlint", // Recommended additional warnings.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xfuture", // Turn on future language features.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match", // Pattern match may not be typesafe.
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates" // Warn if a private member is unused.
  ),
  resolvers += Resolver.sbtPluginRepo("releases") // Fix for "Doc and src packages for 1.3.2 not found in repo1.maven.org" https://github.com/sbt/sbt-native-packager/issues/1063
))

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  releaseStepCommand(sonatypeRelease),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

lazy val publishSettings = Seq(
  publishTo := sonatypePublishTo.value,
  licenses := Seq("MIT License" -> url("https://github.com/dborisenko/sbt-elastic-beanstalk/blob/master/LICENSE")),
  sonatypeProjectHosting := Some(GitHubHosting("dborisenko", "sbt-elastic-beanstalk", "dborisenko@gmail.com")),
  homepage := Some(url("https://github.com/dborisenko/sbt-elastic-beanstalk")),
  scmInfo := Some(ScmInfo(url("https://github.com/dborisenko/sbt-elastic-beanstalk"), "scm:git:git://github.com:dborisenko/sbt-elastic-beanstalk.git")),
  developers := List(Developer(id = "Denis Borisenko", name = "Denis Borisenko", email = "dborisenko@gmail.com", url = url("http://dbrsn.com/")))
)

lazy val `sbt-elastic-beanstalk` = (project in file("."))
  .settings(publishSettings: _*)
  .settings(wartremoverErrors ++= Warts.allBut(Wart.NonUnitStatements, Wart.Any, Wart.Nothing, Wart.Throw, Wart.TraversableOps))
  .settings(
    sbtPlugin := true,
    publishArtifact := true,
    addSbtPlugin(Dependencies.`sbt-native-packager`),
    libraryDependencies ++= Seq(
      Dependencies.`aws-java-sdk-elasticbeanstalk`,
      Dependencies.`aws-java-sdk-s3`
    )
  )
