package io.flow.delta.actors.functions

import javax.inject.Inject

import db.{ShasDao, TagsWriteDao}
import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.api.lib.{Email, GithubUtil, Repo}
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.lib.Semver
import io.flow.delta.v0.models.Project
import io.flow.github.v0.models.{RefForm, TagForm, Tagger}
import io.flow.play.util.Constants
import io.flow.postgresql.Authorization
import org.joda.time.DateTime
import play.api.Application

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
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult] = {
    val tag = app.injector.instanceOf[Tag]
    Future.sequence {
      config.branches.map { branch =>
        tag.run(project, branch.name)
      }
    }.map(SupervisorResult.merge)
  }

}

class Tag @Inject()(
  github: Github,
  shasDao: ShasDao,
  tagsWriteDao: TagsWriteDao,
  email: Email
) {

  private[this] case class Tag(semver: Semver, sha: String)

  private[this] def projectRepo(project: Project): Repo = GithubUtil.parseUri(project.uri).right.getOrElse {
    sys.error(s"Project id[${project.id}] uri[${project.uri}]: Could not parse")
  }

  def run(project: Project, branchName: String)(
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    shasDao.findByProjectIdAndBranch(Authorization.All, project.id, branchName).map(_.hash) match {

      case None => {
        Future {
          SupervisorResult.Error(s"Shas table does not have an entry for $branchName branch")
        }
      }

      case Some(sha) => {
        github.withGithubClient(project.user.id) { client =>
          val repo: Repo = projectRepo(project)

          client.tags.getTags(repo.owner, repo.project).flatMap { tags =>
            GithubUtil.toTags(tags).reverse.headOption match {
              case None => {
                createTag(io.flow.delta.actors.functions.Tag.InitialTag, sha, project, repo)
              }
              case Some(tag) => {
                tag.sha == sha match {
                  case true => {
                    Future {
                      SupervisorResult.Ready(s"Latest tag[${tag.semver.label}] already points to sha[$sha]")
                    }
                  }
                  case false => {
                    createTag(tag.semver.next.label, sha, project, repo)
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
    name: String, sha: String, project: Project, repo: Repo
  ) (
      implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    assert(Semver.isSemver(name), s"Tag[$name] must be in semver format")

    github.withGithubClient(project.user.id) { client =>
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
          tagsWriteDao.upsert(Constants.SystemUser, project.id, name, sha)
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
      }.transform {
        case scala.util.Success(a) => scala.util.Success(a)
        case scala.util.Failure(e) => {
          play.api.Logger.warn(s"creating tag failed for name[$name] sha[$sha] project[$project] repo[$repo]", e)
          scala.util.Failure(e)
        }
      }
    }
  }

}
