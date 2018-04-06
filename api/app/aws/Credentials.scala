package io.flow.delta.aws

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials}
import io.flow.play.util.Config
import play.api.{Environment, Mode}

@javax.inject.Singleton
class Credentials @javax.inject.Inject() (
  config: Config,
  playEnv: Environment
) {

  val aws = playEnv.mode match {
    case Mode.Test => {
      new BasicAWSCredentials("test", "test")
    }

    case _ => {
      new BasicAWSCredentials(
        config.requiredString("aws.delta.access.key"),
        config.requiredString("aws.delta.secret.key")
      )
    }
  }

}
