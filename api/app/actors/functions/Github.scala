package io.flow.delta.actors.functions

import io.flow.github.v0.Client
import io.flow.delta.actors.SupervisorResult
import io.flow.delta.api.lib.GithubHelper
import scala.concurrent.Future

trait Github {

  def withGithubClient(
    userId: String
  ) (
    f: Client => Future[SupervisorResult]
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {

    GithubHelper.apiClientFromUser(userId) match {
      case None => {
        Future {
          SupervisorResult.Error(s"Could not get a client for the project's user[$userId]")
        }
      }
      case Some(client) => {
        f(client)
      }
    }

  }


}
