package com.dbrsn.sbt.ebs

import com.amazonaws.regions.Regions
import com.amazonaws.services.elasticbeanstalk.model._
import com.amazonaws.services.elasticbeanstalk.{AWSElasticBeanstalk, AWSElasticBeanstalkClientBuilder}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._
import com.typesafe.sbt.packager.NativePackagerKeys
import com.typesafe.sbt.packager.docker.{DockerKeys, DockerPlugin}
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._

object ElasticBeanstalkPlugin extends AutoPlugin with NativePackagerKeys with DockerKeys {

  override def requires: DockerPlugin.type = DockerPlugin

  object autoImport extends ElasticBeanstalkKeys {
    val T2: EC2InstanceTypes.T2.type = EC2InstanceTypes.T2
  }

  import autoImport._

  private def buildAWSElasticBeanstalkClient(region: Regions): AWSElasticBeanstalk = {
    val ebClientBuilder = AWSElasticBeanstalkClientBuilder.standard()
    ebClientBuilder.withRegion(region)
    ebClientBuilder.build()
  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    ebsDockerrunVersion := 1,
    ebsVersion := version.value,
    ebsS3AuthBucket := ebsS3Bucket.value,
    ebsS3AuthKey := ".dockercfg",

    ebsContainerPort := 8080,

    ebsContainerMemory := EC2InstanceTypes.T2.Micro.memory,
    ebsContainerMemoryReservation := EC2InstanceTypes.T2.Micro.memoryReservation,
    ebsContainerUseMemoryReservation := true,
    ebsPortMappings := Map(
      80 -> 8080,
      8081 -> 8081
    ),
    ebsEC2InstanceTypes := Set.empty,

    ebsDockerAlias := dockerAlias.value,

    ebsApplicationName := packageName.value,
    ebsEnvironmentName := s"${ebsApplicationName.value}-env",

    ebsVersionsToPublish := {
      ebsDockerrunVersion.value match {
        case 1 =>
          List(ebsVersion.value)
        case 2 =>
          if (ebsEC2InstanceTypes.value.isEmpty) {
            List(ebsVersion.value)
          } else {
            ebsEC2InstanceTypes.value.map { instanceType =>
              s"${ebsVersion.value}-$instanceType"
            }.toList
          }
        case _ => throw new Exception("Invalid setting for 'ebsDockerrunVersion': must be 1 or 2")
      }
    },

    ebsStageDockerrunFiles := {
      val targetValue = target.value
      val ebsDockerAliasValue = ebsDockerAlias.value
      val ebsS3AuthBucketValue = ebsS3AuthBucket.value
      val ebsS3AuthKeyValue = ebsS3AuthKey.value
      val ebsContainerPortValue = ebsContainerPort.value
      val ebsPortMappingsValue = ebsPortMappings.value
      val ebsContainerMemoryValue = ebsContainerMemory.value
      val ebsEC2InstanceTypesValue = ebsEC2InstanceTypes.value
      val ebsContainerUseMemoryReservationValue = ebsContainerUseMemoryReservation.value
      val ebsContainerMemoryReservationValue = ebsContainerMemoryReservation.value

      ebsDockerrunVersion.value match {
        case 1 => List(DockerrunFileGenerator.generateDockerrunFileVersion1(
          targetValue, ebsDockerAliasValue, ebsS3AuthBucketValue, ebsS3AuthKeyValue,
          ebsContainerPortValue
        ))
        case 2 =>
          if (ebsEC2InstanceTypesValue.isEmpty) {
            List(DockerrunFileGenerator.generateDockerrunFileVersion2(
              targetValue, ebsDockerAliasValue, ebsS3AuthBucketValue, ebsS3AuthKeyValue,
              ebsPortMappingsValue, Left((ebsContainerMemoryValue, ebsContainerMemoryReservationValue)),
              ebsContainerUseMemoryReservationValue
            ))
          } else {
            ebsEC2InstanceTypesValue.map { instanceType =>
              DockerrunFileGenerator.generateDockerrunFileVersion2(
                targetValue, ebsDockerAliasValue, ebsS3AuthBucketValue, ebsS3AuthKeyValue,
                ebsPortMappingsValue, Right(instanceType), ebsContainerUseMemoryReservationValue
              )
            }.toList
          }
        case _ => throw new Exception("Invalid setting for 'ebsDockerrunVersion': must be 1 or 2")
      }
    },
    ebsPublishDockerrunFiles := {
      val jsonFiles = ebsStageDockerrunFiles.value
      val bucket = ebsS3Bucket.value
      val ebsApplicationNameValue = ebsApplicationName.value
      val isSnapshotValue = isSnapshot.value

      val s3ClientBuilder = AmazonS3ClientBuilder.standard()
      s3ClientBuilder.withRegion(ebsRegion.value)
      val s3Client = s3ClientBuilder.build()

      if (!s3Client.doesBucketExistV2(bucket)) {
        sLog.value.info(s"Bucket $bucket does not exist. Creating it..")
        s3Client.createBucket(new CreateBucketRequest(bucket))
      }

      jsonFiles.map { jsonFile =>
        val key = s"$ebsApplicationNameValue/${jsonFile.getName}"
        if (s3Client.doesObjectExist(bucket, key) && !isSnapshotValue) {
          throw new Exception(s"Unable to publish new dockerrun file to S3: file $key already exists")
        }
        s3Client.putObject(bucket, key, jsonFile)
      }
    },
    ebsPublishAppVersions := {
      val ebClient = buildAWSElasticBeanstalkClient(ebsRegion.value)
      val ebsApplicationNameValue = ebsApplicationName.value
      val isSnapshotValue = isSnapshot.value
      val ebsVersionValue = ebsVersion.value
      val ebsS3BucketValue = ebsS3Bucket.value

      val applicationDescriptions = ebClient.describeApplications(new DescribeApplicationsRequest().withApplicationNames(ebsApplicationNameValue))

      ebsVersionsToPublish.value.map { versionToPublish =>
        if (applicationDescriptions.getApplications.asScala.exists(_.getVersions contains versionToPublish)) {
          if (isSnapshotValue) {
            ebClient.deleteApplicationVersion(new DeleteApplicationVersionRequest(ebsApplicationNameValue, versionToPublish))
          } else {
            throw new Exception(s"Unable to create new application version on Elastic Beanstalk: version $versionToPublish already exists")
          }
        }

        val key = s"$ebsApplicationNameValue/$versionToPublish.json"
        ebClient.createApplicationVersion(
          new CreateApplicationVersionRequest()
            .withApplicationName(ebsApplicationNameValue)
            .withDescription(ebsVersionValue)
            .withVersionLabel(versionToPublish)
            .withSourceBundle(new S3Location(ebsS3BucketValue, key))
        )
      }
    },
    ebsUpdateEnvironment := {
      val ebClient = buildAWSElasticBeanstalkClient(ebsRegion.value)
      val ebsApplicationNameValue = ebsApplicationName.value

      ebClient.updateEnvironment(
        new UpdateEnvironmentRequest()
          .withApplicationName(ebsApplicationNameValue)
          .withVersionLabel(ebsVersionsToPublish.value.head)
          .withEnvironmentName(ebsEnvironmentName.value)
      )
    },
    ebsDeploy := {
      val _ = {
        ebsUpdateEnvironment.dependsOn(
          ebsPublishAppVersions.dependsOn(
            ebsPublishDockerrunFiles.dependsOn(
              ebsStageDockerrunFiles.dependsOn(
                publish in DockerPlugin.autoImport.Docker
              )
            )
          )
        )
      }.value
    }
  )
}
