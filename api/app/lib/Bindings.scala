package io.flow.delta.api.lib

import play.api.{Environment, Configuration, Mode}
import play.api.inject.Module

class TokenClientModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    Seq(
      bind[io.flow.token.v0.interfaces.Client].to[DefaultTokenClient]
    )
  }
}

class GithubModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    env.mode match {
      case Mode.Prod | Mode.Dev => Seq(
        bind[Github].to[DefaultGithub]
      )
      case Mode.Test => Seq(
        bind[Github].to[MockGithub]
      )
    }
  }

}
