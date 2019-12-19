package io.flow.delta.actors

import javax.inject.Inject
import akka.actor.{Actor, ActorSystem}
import db._
import io.flow.akka.SafeReceive
import io.flow.delta.api.lib.{EventLogProcessor, StateDiff}
import io.flow.delta.config.v0.models.{BuildConfigUndefinedType, BuildStage, EcsBuildConfig, K8sBuildConfig}
import io.flow.delta.config.v0.{models => config}
import io.flow.delta.v0.models.{Build, Version}
import io.flow.log.RollbarLogger
import io.flow.postgresql.Authorization
import play.api.Application

object BuildSupervisorActor {

  trait Message

  object Messages {
    case class CheckTag(name: String) extends Message
    case class Data(id: String) extends Message
    case object PursueDesiredState extends Message
  }

  val Functions = Seq(
    functions.SetDesiredState,
    functions.SyncDockerImages,
    functions.BuildDockerImage,
    functions.Scale,
  )

  trait Factory {
    def apply(id: String): Actor
  }

}

class BuildSupervisorActor @Inject()(
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  override val logger: RollbarLogger,
  buildDesiredStatesDao: BuildDesiredStatesDao,
  eventLogProcessor: EventLogProcessor,
  system: ActorSystem,
  implicit val app: Application,
) extends Actor with DataBuild with DataProject {

  private[this] implicit val ec = system.dispatchers.lookup("supervisor-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("BuildSupervisorActor")

  def receive = SafeReceive.withLogUnhandled {

    case BuildSupervisorActor.Messages.Data(id) =>
      setBuildId(id)

    case BuildSupervisorActor.Messages.PursueDesiredState =>
      withEnabledBuild { build =>
        withBuildConfig { buildConfig =>
          val stages = buildConfig match {
            case c: EcsBuildConfig => c.stages
            case _: K8sBuildConfig => Seq(BuildStage.SetDesiredState)
            case _: BuildConfigUndefinedType => Nil
          }
          eventLogProcessor.runSync("PursueDesiredState", log = log(build.project.id)) {
            run(build, stages, BuildSupervisorActor.Functions)
          } // Should Await the Future?
        }
      }

    /**
      * Indicates that something has happened for the tag with
      * specified name (e.g. 0.0.2). If this tag is in the build's
      * desired state (or ahead of the desired state), triggers
      * PursueDesiredState. Otherwise a no-op.
      */
    case BuildSupervisorActor.Messages.CheckTag(name) =>
      withEnabledBuild { build =>
        buildDesiredStatesDao.findByBuildId(Authorization.All, build.id) match {
          case None => {
            // Might be first tag
            self ! BuildSupervisorActor.Messages.PursueDesiredState
          }
          case Some(state) => {
            StateDiff.up(state.versions, Seq(Version(name, 1))) match {
              case Nil => {
                state.versions.find(_.name == name) match {
                  case None => // no-op
                  case Some(_) => self ! BuildSupervisorActor.Messages.PursueDesiredState
                }
              }
              case _ => self ! BuildSupervisorActor.Messages.PursueDesiredState
            }
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
  private[this] def run(build: Build, stages: Seq[config.BuildStage], functions: Seq[BuildSupervisorFunction]): Unit = {
    functions.headOption match {
      case None => {
        SupervisorResult.Ready("All functions returned without modification")
        ()
      }
      case Some(f) => {
        val projectId = build.project.id

        stages.contains(f.stage) match {
          case false => {
            eventLogProcessor.skipped(s"Stage ${f.stage} is disabled", log = log(projectId))
            run(build, stages, functions.drop(1))
          }
          case true => {
            eventLogProcessor.started(format(f), log = log(projectId))
            f.run(build, requiredBuildConfig).map {
              case SupervisorResult.Change(desc) => {
                eventLogProcessor.changed(format(f, desc), log = log(projectId))
              }
              case SupervisorResult.Checkpoint(desc) => {
                eventLogProcessor.checkpoint(format(f, desc), log = log(projectId))
              }
              case SupervisorResult.Error(desc, ex) => {
                val err = ex.getOrElse {
                  new Exception(desc)
                }
                eventLogProcessor.completed(format(f, desc), Some(err), log = log(projectId))
              }
              case SupervisorResult.Ready(desc) => {
                eventLogProcessor.completed(format(f, desc), log = log(projectId))
                run(build, stages, functions.drop(1))
              }
            }.recover {
              case ex: Throwable => eventLogProcessor.completed(format(f, ex.getMessage), Some(ex), log = log(projectId))
            }
            ()
          }
        }
      }
    }
  }

}
