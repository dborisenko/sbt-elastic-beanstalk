package com.dbrsn.sbt.ebs

import com.amazonaws.regions.Regions
import com.amazonaws.services.elasticbeanstalk.model.{CreateApplicationVersionResult, UpdateEnvironmentResult}
import com.amazonaws.services.s3.model.PutObjectResult
import com.typesafe.sbt.packager.docker.DockerAlias
import sbt.{File, SettingKey, TaskKey, settingKey, taskKey}

trait ElasticBeanstalkKeys {
  lazy val ebsDockerrunVersion: SettingKey[Int] = settingKey[Int]("Version of the Dockerrun file (1 or 2)")
  lazy val ebsVersion: SettingKey[String] = settingKey[String]("Version number of the application to create on Elastic Beanstalk")
  lazy val ebsS3Bucket: SettingKey[String] = settingKey[String]("Name of the S3 bucket to publish docker configurations to")
  lazy val ebsS3AuthBucket: SettingKey[String] = settingKey[String]("Name of the S3 bucket containing the docker auth config")
  lazy val ebsS3AuthKey: SettingKey[String] = settingKey[String]("Name of the S3 file containing the docker auth config")
  lazy val ebsRegion: SettingKey[Regions] = settingKey[Regions]("Region for the Elastic Beanstalk application and S3 bucket")

  lazy val ebsContainerPort: SettingKey[Int] = settingKey[Int]("Docker container port (Dockerrun version 1 only)")

  lazy val ebsContainerMemory: SettingKey[Int] = settingKey[Int]("Memory required by the Docker container (Dockerrun version 2 only)")
  lazy val ebsPortMappings: SettingKey[Map[Int, Int]] = settingKey[Map[Int, Int]]("Port mappings for the Docker container (Dockerrun version 2 only)")
  lazy val ebsEC2InstanceTypes: SettingKey[Set[EC2InstanceType]] = settingKey[Set[EC2InstanceType]](
    "EC2 instance types to generate Dockerrun files for (Dockerrun version 2 only)"
  )

  lazy val ebsDockerAlias: SettingKey[DockerAlias] = settingKey[DockerAlias]("Docker alias descriptor for generating Dockerrun file")

  lazy val ebsContainerMemoryReservation = settingKey[Int]("Memory reservation for the Docker container (Dockerrun version 2 only)")
  lazy val ebsContainerUseMemoryReservation = settingKey[Boolean](
    "Use memoryReservation instead of memory setting for the Docker container (Dockerrun version 2 only)"
  )

  lazy val ebsApplicationName: SettingKey[String] = settingKey[String]("Application name defined in Elastic Beanstalk")
  lazy val ebsEnvironmentName: SettingKey[String] = settingKey[String]("Environment name defined in Elastic Beanstalk")
  lazy val ebsVersionsToPublish: SettingKey[Seq[String]] = settingKey[Seq[String]]("Versions to publish")

  lazy val ebsStageDockerrunFiles: TaskKey[Seq[File]] = taskKey[Seq[File]]("Stages the Dockerrun.aws.json files")
  lazy val ebsPublishDockerrunFiles: TaskKey[Seq[PutObjectResult]] = taskKey[Seq[PutObjectResult]]("Publishes the Dockerrun.aws.json files to an S3 bucket")
  lazy val ebsPublishAppVersions: TaskKey[Seq[CreateApplicationVersionResult]] = taskKey[Seq[CreateApplicationVersionResult]](
    "Publishes the application versions to Elastic Beanstalk"
  )
  lazy val ebsUpdateEnvironment: TaskKey[UpdateEnvironmentResult] = taskKey[UpdateEnvironmentResult](
    "Update application environment to the published app version"
  )

  lazy val ebsDeploy: TaskKey[Unit] = taskKey[Unit]("Deploy application to Elastic Beanstalk environment")
}
