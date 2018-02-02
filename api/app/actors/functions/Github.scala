package io.flow.delta.actors.functions

import javax.inject.Inject

import io.flow.delta.actors.SupervisorResult
import io.flow.delta.api.lib.GitHubHelper
import io.flow.github.v0.Client

import scala.concurrent.Future

class Github @Inject()(
  gitHubHelper: GitHubHelper
) {

  def withGithubClient(
    userId: String
  ) (
    f: Client => Future[SupervisorResult]
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {

    gitHubHelper.apiClientFromUser(userId) match {
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
