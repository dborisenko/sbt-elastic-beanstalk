package com.ovoenergy.sbt.ebs

import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model._
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.typesafe.sbt.packager.NativePackagerKeys
import com.typesafe.sbt.packager.docker.{DockerKeys, DockerPlugin}
import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._

object ElasticBeanstalkPlugin extends AutoPlugin with NativePackagerKeys with DockerKeys {

  override def requires = DockerPlugin

  object autoImport {
    lazy val ebsVersion = settingKey[String]("Version number of the application to create on Elastic Beanstalk")
    lazy val ebsS3Bucket = settingKey[String]("Name of the S3 bucket to publish docker configurations to")
    lazy val ebsS3AuthBucket = settingKey[String]("Name of the S3 bucket containing the docker auth config")
    lazy val ebsS3AuthKey = settingKey[String]("Name of the S3 file containing the docker auth config")
    lazy val ebsRegion = settingKey[Regions]("Region for the Elastic Beanstalk application and S3 bucket")

    lazy val ebsStageDockerrunFile = taskKey[File]("Stages the Dockerrun.aws.json file")
    lazy val ebsPublishDockerrunFile = taskKey[PutObjectResult]("Publishes the Dockerrun.aws.json file an S3 bucket")
    lazy val ebsPublishAppVersion = taskKey[CreateApplicationVersionResult]("Publishes the new application version on Elastic Beanstalk")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    ebsVersion := version.value,
    ebsS3AuthBucket := ebsS3Bucket.value,
    ebsS3AuthKey := ".dockercfg",

    ebsStageDockerrunFile := {
      val jsonFile = target.value / "aws" / "Dockerrun.aws.json"
      jsonFile.delete()
      IO.write(jsonFile, dockerrunFile.value)
      jsonFile
    },
    ebsPublishDockerrunFile := {
      val key = s"${packageName.value}/${version.value}.json"
      val jsonFile = ebsStageDockerrunFile.value

      val bucket = ebsS3Bucket.value

      val s3Client = new AmazonS3Client()
      s3Client.setRegion(Region.getRegion(ebsRegion.value))

      if (!s3Client.doesBucketExist(bucket)) {
        println(s"Bucket $bucket does not exist. Creating it..")
        s3Client.createBucket(new CreateBucketRequest(bucket))
      }

      s3Client.putObject(bucket, key, jsonFile)
    },
    ebsPublishAppVersion := {
      val key = s"${packageName.value}/${version.value}.json"

      val ebClient = new AWSElasticBeanstalkClient()
      ebClient.setRegion(Region.getRegion(ebsRegion.value))

      val applicationDescriptions = ebClient.describeApplications(new DescribeApplicationsRequest().withApplicationNames(packageName.value))

      if (applicationDescriptions.getApplications.exists(_.getVersions contains version.value)) {
        if(isSnapshot.value) ebClient.deleteApplicationVersion(new DeleteApplicationVersionRequest(packageName.value, version.value))
        else throw new Exception(s"Unable to create new application version on Elastic Beanstalk: version ${version.value} already exists")
      }

      ebClient.createApplicationVersion(
        new CreateApplicationVersionRequest()
        .withApplicationName(packageName.value)
        .withDescription(version.value)
        .withVersionLabel(ebsVersion.value)
        .withSourceBundle(new S3Location(ebsS3Bucket.value, key))
      )
    }
  )

  lazy val dockerrunFile: Def.Initialize[String] = Def.setting {
    val image = s"${packageName.value}:${version.value}"
    val imageName = dockerRepository.value match {
      case Some(repository) => s"$repository/$image"
      case None => image
    }
    s"""|{
        |  "AWSEBDockerrunVersion": "1",
        |  "Image": {
        |    "Name": "$imageName"
        |  },
        |  "Authentication": {
        |    "Bucket": "${ebsS3AuthBucket.value}",
        |    "Key": "${ebsS3AuthKey.value}"
        |  },
        |  "Ports": [
        |    {
        |      "ContainerPort": "8080"
        |    }
        |  ]
        |}""".stripMargin
  }
}
