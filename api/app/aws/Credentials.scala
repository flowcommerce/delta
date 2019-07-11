package io.flow.delta.aws

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import io.flow.play.util.Config
import play.api.{Environment, Mode}

@javax.inject.Singleton
class Credentials @javax.inject.Inject() (
  config: Config,
  playEnv: Environment
) {

  val aws = playEnv.mode match {
    case Mode.Test => {
      AwsBasicCredentials.create("test", "test")
    }

    case _ => {
      AwsBasicCredentials.create(
        config.requiredString("aws.delta.access.key"),
        config.requiredString("aws.delta.secret.key")
      )
    }
  }

}
