package io.flow.delta.actors.functions

import db.{ShasDao, TagsDao, TagsWriteDao, UsersDao}
import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.api.lib.{Email, GithubUtil}
import io.flow.delta.lib.Semver
import io.flow.delta.v0.models.Project
import io.flow.github.v0.models.{RefForm, TagForm, Tagger}
import io.flow.postgresql.Authorization
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.Future

/**
  * If there is no tag pointing to the master sha, creates a tag in
  * github and records it here.
  */
object Tag extends ProjectSupervisorFunction {

  val InitialTag = "0.0.1"

  override val stage = ProjectStage.Tag

  override def run(
    project: Project,
    config: ConfigProject
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    Future.sequence {
      config.branches.map { branch =>
        Tag(project, branch.name).run
      }
    }.map {
      SupervisorResult.merge(_)
    }
  }

}

case class Tag(project: Project, branchName: String) extends Github {

  private[this] lazy val tagsWriteDao = play.api.Play.current.injector.instanceOf[TagsWriteDao]

  private[this] case class Tag(semver: Semver, sha: String)

  private[this] val repo = GithubUtil.parseUri(project.uri).right.getOrElse {
    sys.error(s"Project id[${project.id}] uri[${project.uri}]: Could not parse")
  }

  private[this] val email = play.api.Play.current.injector.instanceOf[Email]

  def run(
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    ShasDao.findByProjectIdAndBranch(Authorization.All, project.id, branchName).map(_.hash) match {

      case None => {
        Future {
          SupervisorResult.Error(s"Shas table does not have an entry for $branchName branch")
        }
      }

      case Some(sha) => {
        withGithubClient(project.user.id) { client =>
          client.tags.getTags(repo.owner, repo.project).flatMap { tags =>
            GithubUtil.toTags(tags).reverse.headOption match {
              case None => {
                createTag(io.flow.delta.actors.functions.Tag.InitialTag, sha)
              }
              case Some(tag) => {
                tag.sha == sha match {
                  case true => {
                    Future {
                      SupervisorResult.Ready(s"Latest tag[${tag.semver.label}] already points to sha[$sha]")
                    }
                  }
                  case false => {
                    createTag(tag.semver.next.label, sha)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
    * For projects with auto tag enabled, this method actually creates
    * a new tag with the given name, pointing to the specified sha.
    * 
    * @param name e.g. 0.0.2
    * @param sha e.g. ff731cfdad6e5b05ec40535fd7db03c91bbcb8ff
    */
  private[this] def createTag(
    name: String, sha: String
  ) (
      implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    assert(Semver.isSemver(name), s"Tag[$name] must be in semver format")

    withGithubClient(project.user.id) { client =>
      client.tags.postGitAndTags(
        repo.owner,
        repo.project,
        TagForm(
          tag = name,
          message = s"Delta automated tag $name",
          `object` = sha,
          tagger = Tagger(
            name = Seq(email.fromName.first, email.fromName.last).flatten.mkString(" "),
            email = email.fromEmail,
            date = new DateTime()
          )
        )
      ).flatMap { githubTag =>
        client.refs.post(
          repo.owner,
          repo.project,
          RefForm(
            ref = s"refs/tags/$name",
            sha = sha
          )
        ).map { githubRef =>
          tagsWriteDao.upsert(UsersDao.systemUser, project.id, name, sha)
          SupervisorResult.Change(s"Created tag $name for sha[$sha]")
        }.recover {
          case e: io.flow.github.v0.errors.UnprocessableEntityResponse => {
            e.unprocessableEntity.message match {
              case "Reference already exists" => {
                SupervisorResult.Ready(s"Tag $name for sha[$sha] already exists")
              }
              case other => {
                SupervisorResult.Error(s"422 creating ref: $other")
              }
            }
          }
        }
      }
    }
  }

}
