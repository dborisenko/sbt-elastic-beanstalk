package com.ovoenergy.sbt.ebs

import sbt._
import Keys._

import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model._
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.typesafe.sbt.packager.NativePackagerKeys
import com.typesafe.sbt.packager.docker.{DockerKeys, DockerPlugin}

import scala.collection.JavaConversions._

object ElasticBeanstalkPlugin extends AutoPlugin with NativePackagerKeys with DockerKeys {

  override def requires = DockerPlugin

  object autoImport {
    lazy val ebsDockerrunVersion = settingKey[Int]("Version of the Dockerrun file (1 or 2)")
    lazy val ebsVersion = settingKey[String]("Version number of the application to create on Elastic Beanstalk")
    lazy val ebsS3Bucket = settingKey[String]("Name of the S3 bucket to publish docker configurations to")
    lazy val ebsS3AuthBucket = settingKey[String]("Name of the S3 bucket containing the docker auth config")
    lazy val ebsS3AuthKey = settingKey[String]("Name of the S3 file containing the docker auth config")
    lazy val ebsRegion = settingKey[Regions]("Region for the Elastic Beanstalk application and S3 bucket")

    lazy val ebsContainerPort = settingKey[Int]("Docker container port (Dockerrun version 1 only)")

    lazy val ebsContainerMemory = settingKey[Int]("Memory required by the Docker container (Dockerrun version 2 only)")
    lazy val ebsPortMappings = settingKey[Map[Int, Int]]("Port mappings for the Docker container (Dockerrun version 2 only)")
    lazy val ebsEC2InstanceTypes = settingKey[Set[EC2InstanceType]]("EC2 instance types to generate Dockerrun files for (Dockerrun version 2 only)")

    lazy val ebsStageDockerrunFiles = taskKey[List[File]]("Stages the Dockerrun.aws.json files")
    lazy val ebsPublishDockerrunFiles = taskKey[List[PutObjectResult]]("Publishes the Dockerrun.aws.json files to an S3 bucket")
    lazy val ebsPublishAppVersions = taskKey[List[CreateApplicationVersionResult]]("Publishes the application versions to Elastic Beanstalk")

    val T2 = EC2InstanceTypes.T2
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    ebsDockerrunVersion := 1,
    ebsVersion := version.value,
    ebsS3AuthBucket := ebsS3Bucket.value,
    ebsS3AuthKey := ".dockercfg",

    ebsContainerPort := 8080,

    ebsContainerMemory := 512,
    ebsPortMappings := Map(
      80 -> 8080,
      8081 -> 8081
    ),
    ebsEC2InstanceTypes := Set.empty,

    ebsStageDockerrunFiles := {

      ebsDockerrunVersion.value match {
        case 1 => List(DockerrunFileGenerator.generateDockerrunFileVersion1(
          target.value, packageName.value, version.value,
            dockerRepository.value, ebsS3AuthBucket.value, ebsS3AuthKey.value,
            ebsContainerPort.value))
        case 2 =>
          if(ebsEC2InstanceTypes.value.isEmpty) {
            List(DockerrunFileGenerator.generateDockerrunFileVersion2(
              target.value, packageName.value, version.value,
                dockerRepository.value, ebsS3AuthBucket.value, ebsS3AuthKey.value,
                ebsPortMappings.value, Left(ebsContainerMemory.value)))
          } else {
            ebsEC2InstanceTypes.value.map { instanceType =>
              DockerrunFileGenerator.generateDockerrunFileVersion2(
                target.value, packageName.value, version.value,
                  dockerRepository.value, ebsS3AuthBucket.value, ebsS3AuthKey.value,
                  ebsPortMappings.value, Right(instanceType))
            }.toList
          }
        case _ => throw new Exception("Invalid setting for 'ebsDockerrunVersion': must be 1 or 2")
      }
    },
    ebsPublishDockerrunFiles := {
      val jsonFiles = ebsStageDockerrunFiles.value

      val bucket = ebsS3Bucket.value

      val s3Client = new AmazonS3Client()
      s3Client.setRegion(Region.getRegion(ebsRegion.value))

      if (!s3Client.doesBucketExist(bucket)) {
        println(s"Bucket $bucket does not exist. Creating it..")
        s3Client.createBucket(new CreateBucketRequest(bucket))
      }

      jsonFiles.map { jsonFile =>
        val key = s"${packageName.value}/${jsonFile.getName}"
        if(s3Client.doesObjectExist(bucket, key) && !isSnapshot.value) {
          throw new Exception(s"Unable to publish new dockerrun file to S3: file $key already exists")
        }
        s3Client.putObject(bucket, key, jsonFile)
      }
    },
    ebsPublishAppVersions := {

      val versionsToPublish = ebsDockerrunVersion.value match {
        case 1 => List(version.value)
        case 2 =>
          if(ebsEC2InstanceTypes.value.isEmpty) List(version.value)
          else ebsEC2InstanceTypes.value.map { instanceType =>
            s"${version.value}-${instanceType}"
          }.toList
        case _ => throw new Exception("Invalid setting for 'ebsDockerrunVersion': must be 1 or 2")
      }

      val ebClient = new AWSElasticBeanstalkClient()
      ebClient.setRegion(Region.getRegion(ebsRegion.value))

      val applicationDescriptions = ebClient.describeApplications(new DescribeApplicationsRequest().withApplicationNames(packageName.value))

      versionsToPublish.map { ebsVersion =>
        if (applicationDescriptions.getApplications.exists(_.getVersions contains ebsVersion)) {
          if(isSnapshot.value) ebClient.deleteApplicationVersion(new DeleteApplicationVersionRequest(packageName.value, ebsVersion))
          else throw new Exception(s"Unable to create new application version on Elastic Beanstalk: version $ebsVersion already exists")
        }

        val key = s"${packageName.value}/${ebsVersion}.json"
        ebClient.createApplicationVersion(
          new CreateApplicationVersionRequest()
          .withApplicationName(packageName.value)
          .withDescription(version.value)
          .withVersionLabel(ebsVersion)
          .withSourceBundle(new S3Location(ebsS3Bucket.value, key))
        )
      }
    }
  )
}
