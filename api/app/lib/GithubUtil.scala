package io.flow.delta.api.lib

import io.flow.delta.lib.Semver
import io.flow.github.v0.models.TagSummary

object GithubUtil {

  def parseUri(uri: String): Either[String, Repo] = {
    Validation.validateUri(uri) match {
      case Left(errors) => {
        Left(errors.mkString(", "))
      }
      case Right(u) => {
        val path = if (u.getPath.startsWith("/")) {
          u.getPath.substring(1)
        } else {
          u.getPath
        }.trim
        path.split("/").filter(!_.isEmpty).toList match {
          case Nil => Left(s"URI path cannot be empty for uri[$uri]")
          case _ :: Nil => Left(s"Invalid uri path[$uri] missing project name")
          case owner :: project :: Nil => Right(Repo(owner, project))
          case _ => Left(s"Invalid uri path[$u] - desired exactly two path components")
        }
      }
    }
  }

  case class Tag(semver: Semver, sha: String)
  
  /**
    * Given a list of tag summaries from github, selects out the tags
    * that are semver, sorts them, and maps to our internal Tag class
    * instances
    */
  def toTags(tags: Seq[TagSummary]): Seq[Tag] = {
    tags.
      flatMap { t =>
        Semver.parse(t.name).map( semvar => Tag(semvar, t.commit.sha) )
      }.
      sortBy { _.semver }
  }
}
