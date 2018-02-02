package io.flow.delta.actors

import javax.inject.Inject

import akka.actor.{Actor, ActorSystem}
import db.{BuildsDao, ConfigsDao, OrganizationsDao, ProjectsDao}
import io.flow.delta.api.lib.EventLogProcessor
import io.flow.delta.config.v0.models.ConfigProject
import io.flow.delta.v0.models.Project
import io.flow.play.actors.ErrorHandler
import io.flow.postgresql.Authorization
import play.api.{Application, Logger}

object ProjectSupervisorActor {

  trait Message

  object Messages {
    case class Data(id: String) extends Message

    case class CheckTag(name: String) extends Message
    case object PursueDesiredState extends Message
  }

  val Functions = Seq(
    functions.SyncShas,
    functions.SyncTags,
    functions.Tag
  )

}

class ProjectSupervisorActor @Inject()(
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  eventLogProcessor: EventLogProcessor,
  system: ActorSystem,
  implicit val app: Application
) extends Actor with ErrorHandler with DataBuild with DataProject with EventLog {

  private[this] implicit val ec = system.dispatchers.lookup("supervisor-actor-context")

  def receive = {

    case msg @ ProjectSupervisorActor.Messages.Data(id) => withErrorHandler(msg) {
      setProjectId(id)
    }

    case msg @ ProjectSupervisorActor.Messages.PursueDesiredState => withErrorHandler(msg) {
      withProject { project =>
        Logger.info(s"PursueDesiredState project[${project.id}]")
        withConfig { config =>
          Logger.info(s"  - config: $config")
          eventLogProcessor.runSync("PursueDesiredState", log = log) {
            run(project, config, ProjectSupervisorActor.Functions)

            buildsDao.findAllByProjectId(Authorization.All, project.id).foreach { build =>
              sender ! MainActor.Messages.BuildSync(build.id)
            }
          }
        }
      }
    }

    case msg @ ProjectSupervisorActor.Messages.CheckTag(name: String) => withErrorHandler(msg) {
      withProject { project =>
        buildsDao.findAllByProjectId(Authorization.All, project.id).foreach { build =>
          sender ! MainActor.Messages.BuildCheckTag(build.id, name)
        }
      }
    }

  }

  /**
    * Sequentially runs through the list of functions. If any of the
    * functions returns a SupervisorResult.Changed or
    * SupervisorResult.Error, returns that result. Otherwise will
    * return Ready at the end of all the functions.
    */
  private[this] def run(project: Project, config: ConfigProject, functions: Seq[ProjectSupervisorFunction]) {
    functions.headOption match {
      case None => {
        SupervisorResult.Ready("All functions returned without modification")
      }
      case Some(f) => {
        if (config.stages.contains(f.stage)) {
          eventLogProcessor.started(format(f), log = log)
          f.run(project, config).map {
            case SupervisorResult.Change(desc) => {
              eventLogProcessor.changed(format(f, desc), log = log)
            }
            case SupervisorResult.Checkpoint(desc) => {
              eventLogProcessor.checkpoint(format(f, desc), log = log)
            }
            case SupervisorResult.Error(desc, ex)=> {
              val err = ex.getOrElse {
                new Exception(desc)
              }
              eventLogProcessor.completed(format(f, desc), Some(err), log = log)
            }
            case SupervisorResult.Ready(desc)=> {
              eventLogProcessor.completed(format(f, desc), log = log)
              run(project, config, functions.drop(1))
            }

          }.recover {
            case ex: Throwable => eventLogProcessor.completed(format(f, ex.getMessage), Some(ex), log = log)
          }
        } else {
          eventLogProcessor.skipped(s"Stage ${f.stage} is disabled", log = log)
          run(project, config, functions.drop(1))
        }
      }
    }
  }

}
