package io.flow.delta.api.lib

import io.flow.play.clients.DefaultTokenClient
import k8s.{DefaultKubernetesService, DummyK8sService, KubernetesService}
import play.api.inject.Module
import play.api.{Configuration, Environment, Mode}

class TokenClientModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    Seq(
      bind[io.flow.token.v0.interfaces.Client].to[DefaultTokenClient]
    )
  }
}

class KubernetesModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    env.mode match {
      case Mode.Prod | Mode.Dev => Seq(
        bind[DefaultKubernetesService].to[KubernetesService]
      )
      case Mode.Test => Seq(
        bind[DummyK8sService].to[KubernetesService]
      )
    }
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
