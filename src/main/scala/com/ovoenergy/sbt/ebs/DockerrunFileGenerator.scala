package com.ovoenergy.sbt.ebs

import sbt._

object DockerrunFileGenerator {

  def generateDockerrunFileVersion1(targetDir: File, packageName: String, version: String,
    dockerRepository: Option[String], s3AuthBucket: String, s3AuthKey: String,
    containerPort: Int): File = {

    val fileName = s"$version.json"
    val jsonFile = targetDir / "aws" / fileName
    jsonFile.delete()

    val image = s"$packageName:$version"
    val imageName = dockerRepository match {
      case Some(repository) => s"$repository/$image"
      case None => image
    }
    val fileContents =
      s"""|{
          |  "AWSEBDockerrunVersion": "1",
          |  "Image": {
          |    "Name": "$imageName"
          |  },
          |  "Authentication": {
          |    "Bucket": "$s3AuthBucket",
          |    "Key": "$s3AuthKey"
          |  },
          |  "Ports": [
          |    {
          |      "ContainerPort": "$containerPort"
          |    }
          |  ]
          |}""".stripMargin

    IO.write(jsonFile, fileContents)
    jsonFile
  }

  def generateDockerrunFileVersion2(targetDir: File, packageName: String, version: String,
    dockerRepository: Option[String], s3AuthBucket: String, s3AuthKey: String,
    portMappings: Map[Int, Int], memoryOrInstanceType: Either[Int, EC2InstanceType]): File = {

    val fileName = memoryOrInstanceType match {
      case Left(_) => s"$version.json"
      case Right(instanceType) => s"$version-$instanceType.json"
    }
    val jsonFile = targetDir / "aws" / fileName
    jsonFile.delete()

    val image = s"$packageName:$version"
    val imageName = dockerRepository match {
      case Some(repository) => s"$repository/$image"
      case None => image
    }

    import EC2InstanceTypes._
    val memory = memoryOrInstanceType match {
      case Left(mem) => mem
      case Right(instanceType) => instanceType.memory
    }
    val fileContents =
      s"""|{
          |  "AWSEBDockerrunVersion": 2,
          |  "authentication": {
          |    "bucket": "$s3AuthBucket",
          |    "key": "$s3AuthKey"
          |  },
          |  "containerDefinitions": [
          |    {
          |      "name": "$packageName",
          |      "image": "$imageName",
          |      "memory": $memory,
          |      "essential": true,
          |      "portMappings": [
          |""".stripMargin + portMappings.map { case (hostPort, containerPort) =>
      s"""|        {
          |          "hostPort": $hostPort,
          |          "containerPort": $containerPort
          |        }""".stripMargin }.mkString(",\n") + '\n' +
      s"""|      ]
          |    }
          |  ]
          |}""".stripMargin

    IO.write(jsonFile, fileContents)
    jsonFile
  }

}
