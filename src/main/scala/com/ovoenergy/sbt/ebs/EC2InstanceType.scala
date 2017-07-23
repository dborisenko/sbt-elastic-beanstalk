package com.ovoenergy.sbt.ebs

sealed trait EC2InstanceType {
  val memory: Int
}
object EC2InstanceTypes {
  object T2 {
    case object Micro extends EC2InstanceType {
      override def toString = "t2.micro"
      val memory = 512
    }
    case object Small extends EC2InstanceType {
      override def toString = "t2.small"
      val memory = 1024
    }
    case object Medium extends EC2InstanceType {
      override def toString = "t2.medium"
      val memory = 2048
    }
    case object Large extends EC2InstanceType {
      override def toString = "t2.large"
      val memory = 4096
    }
  }
}
