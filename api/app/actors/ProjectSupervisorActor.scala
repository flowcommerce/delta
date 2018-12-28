package io.flow.delta.actors

import javax.inject.Inject
import akka.actor.{Actor, ActorSystem}
import com.google.inject.assistedinject.Assisted
import db.{BuildsDao, ConfigsDao, OrganizationsDao, ProjectsDao}
import io.flow.akka.SafeReceive
import io.flow.delta.api.lib.EventLogProcessor
import io.flow.delta.config.v0.models.ConfigProject
import io.flow.delta.v0.models.Project
import io.flow.log.RollbarLogger
import io.flow.postgresql.Authorization
import play.api.Application

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

  trait Factory {
    def apply(id: String): Actor
  }

}

class ProjectSupervisorActor @Inject()(
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  override val logger: RollbarLogger,
  eventLogProcessor: EventLogProcessor,
  system: ActorSystem,
  implicit val app: Application,
  @Assisted id: String
) extends Actor with DataBuild with DataProject with EventLog {

  private[this] implicit val ec = system.dispatchers.lookup("supervisor-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("ProjectSupervisorActor")

  def receive = SafeReceive {

    case ProjectSupervisorActor.Messages.Data(id) =>
      setProjectId(id)

    case ProjectSupervisorActor.Messages.PursueDesiredState =>
      withProject { project =>
        withConfig { config =>
          logger.
            fingerprint("ProjectSupervisorActor").
            withKeyValue("project_id", project.id).
            withKeyValue("config", config.toString).
            info(s"PursueDesiredState starting")

          eventLogProcessor.runSync("PursueDesiredState", log = log(id)) {
            run(project, config, ProjectSupervisorActor.Functions)

            buildsDao.findAllByProjectId(Authorization.All, project.id).foreach { build =>
              sender ! MainActor.Messages.BuildSync(build.id)
            }
          }
        }
      }
      ()

    case ProjectSupervisorActor.Messages.CheckTag(name: String) =>
      withProject { project =>
        buildsDao.findAllByProjectId(Authorization.All, project.id).foreach { build =>
          sender ! MainActor.Messages.BuildCheckTag(build.id, name)
        }
      }
      ()

  }

  /**
    * Sequentially runs through the list of functions. If any of the
    * functions returns a SupervisorResult.Changed or
    * SupervisorResult.Error, returns that result. Otherwise will
    * return Ready at the end of all the functions.
    */
  private[this] def run(project: Project, config: ConfigProject, functions: Seq[ProjectSupervisorFunction]): Unit = {
    functions.headOption match {
      case None => {
        SupervisorResult.Ready("All functions returned without modification")
        ()
      }
      case Some(f) => {
        if (config.stages.contains(f.stage)) {
          eventLogProcessor.started(format(f), log = log(project.id))
          f.run(project, config).map {
            case SupervisorResult.Change(desc) => {
              eventLogProcessor.changed(format(f, desc), log = log(project.id))
            }
            case SupervisorResult.Checkpoint(desc) => {
              eventLogProcessor.checkpoint(format(f, desc), log = log(project.id))
            }
            case SupervisorResult.Error(desc, ex)=> {
              val err = ex.getOrElse {
                new Exception(desc)
              }
              eventLogProcessor.completed(format(f, desc), Some(err), log = log(project.id))
            }
            case SupervisorResult.Ready(desc)=> {
              eventLogProcessor.completed(format(f, desc), log = log(project.id))
              run(project, config, functions.drop(1))
            }

          }.recover {
            case ex: Throwable => eventLogProcessor.completed(format(f, ex.getMessage), Some(ex), log = log(project.id))
          }
          () // Should Await the Future?
        } else {
          eventLogProcessor.skipped(s"Stage ${f.stage} is disabled", log = log(project.id))
          run(project, config, functions.drop(1))
        }
      }
    }
  }

}
