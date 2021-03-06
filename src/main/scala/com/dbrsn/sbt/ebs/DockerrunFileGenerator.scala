package com.dbrsn.sbt.ebs

import com.typesafe.sbt.packager.docker.DockerAlias
import sbt._

object DockerrunFileGenerator {

  def generateDockerrunFileVersion1(targetDir: File, docker: DockerAlias, s3AuthBucket: String, s3AuthKey: String,
    containerPort: Int): File = {

    val fileName = s"${docker.tag.getOrElse("latest")}.json"
    val jsonFile = targetDir / "aws" / fileName
    jsonFile.delete()

    val fileContents =
      s"""|{
          |  "AWSEBDockerrunVersion": "1",
          |  "Image": {
          |    "Name": "${docker.versioned}"
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

  def generateDockerrunFileVersion2(targetDir: File, docker: DockerAlias, s3AuthBucket: String, s3AuthKey: String,
    portMappings: Map[Int, Int], memoryOrInstanceType: Either[(Int, Int), EC2InstanceType],
    useMemoryReservation: Boolean): File = {

    val version = docker.tag.getOrElse("latest")
    val fileName = memoryOrInstanceType match {
      case Left(_) => s"$version.json"
      case Right(instanceType) => s"$version-$instanceType.json"
    }
    val jsonFile = targetDir / "aws" / fileName
    jsonFile.delete()
    val memory = memoryOrInstanceType match {
      case Left((mem, _)) => mem
      case Right(instanceType) => instanceType.memory
    }
    val memoryReservation = memoryOrInstanceType match {
      case Left((_, mr)) => mr
      case Right(instanceType) => instanceType.memoryReservation
    }
    val memorySettingField = if (useMemoryReservation) "memoryReservation" else "memory"
    val memorySettingValue = if (useMemoryReservation) memoryReservation else memory
    val fileContents =
      s"""|{
          |  "AWSEBDockerrunVersion": 2,
          |  "authentication": {
          |    "bucket": "$s3AuthBucket",
          |    "key": "$s3AuthKey"
          |  },
          |  "containerDefinitions": [
          |    {
          |      "name": "${docker.name}",
          |      "image": "${docker.versioned}",
          |      "memory": $memory,
          |      "$memorySettingField": $memorySettingValue,
          |      "essential": true,
          |      "portMappings": [
          |""".stripMargin + portMappings.map {
        case (hostPort, containerPort) =>
          s"""|        {
              |          "hostPort": $hostPort,
              |          "containerPort": $containerPort
              |        }""".stripMargin
      }.mkString(",\n") + "\n" +
        s"""|      ]
            |    }
            |  ]
            |}""".stripMargin

    IO.write(jsonFile, fileContents)
    jsonFile
  }

}
