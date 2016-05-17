# sbt-elastic-beanstalk

SBT plugin providing support for publishing Docker applications to Amazon
Elastic Beanstalk.

Supports AWS Dockerrun file versions 1 and 2.

Version 2 can be used to expose multiple service ports for a single or multi
container image.

## Installation

Add the following to you ```project/plugins.sbt``` file

    addSbtPlugin("com.ovoenergy" % "sbt-elastic-beanstalk" % "0.2.0")

## Plugin Configuration

See [ElasticBeanstalkPlugin.scala](src/main/scala/com/ovoenergy/sbt/ebs/ElasticBeanstalkPlugin.scala)
for configuration options and default values.

## Plugin Tasks

The plugin provides the following tasks

* ```ebsStageDockerrunFiles``` - stages dockerrun files to ```project-name/target/aws/```
* ```ebsPublishDockerrunFiles``` - publishes dockerrun files to S3
* ```ebsPublishAppVersions``` - publish current application version to elasticbeanstalk


### ```ebsStageDockerrunFiles```

Use this task to locally inspect the dockerrun files to be published to AWS.

### ```ebsPublishDockerrunFiles```

This task publishes the dockerrun files to the configured S3 bucket.

### ```ebsPublishAppVersions```

This task associates the application version with the dockerrun files which then
makes them available to elastic beanstalk for depoloyment.

This step depends on ```ebsPublishDockerrunFiles``` which must be run first.

## Dockerrun files

Dockerrun files contain JSON formatted configuration, and are specific to
Elastic Beanstalk. They describe how to deploy a docker container as an Elastic
Beanstalk application.

### Dockerrun V1

Version 1 should typically be used to define single container docker
applications that expose a single port.

Single container applications that need to expose multiple ports should use the
V2 specification instead.

See [AWS Dockerrun V1](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/create_deploy_docker_image.html#create_deploy_docker_image_dockerrun)
for further information regarding the V1 format.

### Dockerrun V2

Version 2 defines applications with

* one or more docker containers
* more than one service port

By default the plugin supports a single container docker application exposing
multiple service ports.

Version 2 includes a mandatory container memory allocation which is set
automatically according to the AWS Instance type.

> *Note* - Instance types must be configured in ```ebsEC2InstanceTypes```.

Sample dockerrun file for an application exposing 3 service ports running on a
t2 small instance.

    {
      "AWSEBDockerrunVersion": 2,
      "authentication": {
        "bucket": "ovo-docker-apps",
        "key": ".dockercfg"
      },
      "containerDefinitions": [
        {
          "name": "onboarding-service",
          "image": "ovotech/onboarding-service:0.1.36",
          "memory": 1024,
          "essential": true,
          "portMappings": [
            {
              "hostPort": 80,
              "containerPort": 8080
            },
            {
              "hostPort": 8081,
              "containerPort": 8081
            },
            {
              "hostPort": 2551,
              "containerPort": 2551
            }
          ]
        }
      ]
    }

> *A note about memory*
>
> In the example above we see that memory is set to 1024MiB. However a T2 Small
> instance has 2048MiB.
>
> The plugin must set memory here to half the amount because this value is
> passed to the docker run command via the -m option. By default docker will
> set the cgroup configuration value ```memory.memsw.limit_in_bytes``` to
> double the value passed in via the -m parameter.
>
> For this reason we must specify half the amount we actaully want to allocate.
>
> The plugin uses memory values defined in [EC2InstanceTypes](src/main/scala/com/ovoenergy/sbt/ebs/EC2IntanceType.scala)
> which assume that a single application wishes to be allocated all memory
> available on a given instance.
>
> Refer to links at the end of this README for further information.

See [AWS Dockerrun V2](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/create_deploy_docker_v2config.html#create_deploy_docker_v2config_dockerrun)
for further information regarding the V2 format.

## Build Pipeline

See [sbt-elastic-beanstalk](http://ob-go.ovotech.org.uk/go/tab/pipeline/history/sbt-elastic-beanstalk)

## See Also

* [How to Deploy Docker Apps to Elastic BeanStalk](https://github.com/hopsoft/relay/wiki/How-to-Deploy-Docker-apps-to-Elastic-Beanstalk)
* [Elastic Beanstalk - Working With Docker](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/create_deploy_docker.html)
* [Managing Docker Memory Shares](https://goldmann.pl/blog/2014/09/11/resource-management-in-docker/#_example_managing_the_memory_shares_of_a_container)
* [```cgroups``` Memory Configuration](https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Resource_Management_Guide/sec-memory.html)
