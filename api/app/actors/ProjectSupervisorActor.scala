package io.flow.delta.actors

import akka.actor.Actor
import db.{BuildsDao, BuildDesiredStatesDao, ConfigsDao}
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.v0.models.{Project, Version}
import io.flow.play.actors.ErrorHandler
import io.flow.postgresql.Authorization
import play.api.Logger
import play.libs.Akka

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

class ProjectSupervisorActor extends Actor with ErrorHandler with DataProject with EventLog {

  private[this] implicit val ec = Akka.system.dispatchers.lookup("supervisor-actor-context")

  override lazy val configsDao = play.api.Play.current.injector.instanceOf[ConfigsDao]

  def receive = {

    case msg @ ProjectSupervisorActor.Messages.Data(id) => withErrorHandler(msg) {
      setProjectId(id)
    }

    case msg @ ProjectSupervisorActor.Messages.PursueDesiredState => withErrorHandler(msg) {
      withProject { project =>
        Logger.info(s"PursueDesiredState project[${project.id}]")
        withConfig { config =>
          Logger.info(s"  - config: $config")
          log.runSync("PursueDesiredState") {
            run(project, config, ProjectSupervisorActor.Functions)

            BuildsDao.findAllByProjectId(Authorization.All, project.id).foreach { build =>
              sender ! MainActor.Messages.BuildSync(build.id)
            }
          }
        }
      }
    }

    case msg @ ProjectSupervisorActor.Messages.CheckTag(name: String) => withErrorHandler(msg) {
      withProject { project =>
        BuildsDao.findAllByProjectId(Authorization.All, project.id).map { build =>
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
        config.stages.contains(f.stage) match {
          case false => {
            log.skipped(s"Stage ${f.stage} is disabled")
            run(project, config, functions.drop(1))
          }
          case true => {
            log.started(format(f))
            f.run(project, config).map { result =>
              result match {
                case SupervisorResult.Change(desc) => {
                  log.changed(format(f, desc))
                }
                case SupervisorResult.Checkpoint(desc) => {
                  log.checkpoint(format(f, desc))
                }
                case SupervisorResult.Error(desc, ex)=> {
                  val err = ex.getOrElse {
                    new Exception(desc)
                  }
                  log.completed(format(f, desc), Some(err))
                }
                case SupervisorResult.Ready(desc)=> {
                  log.completed(format(f, desc))
                  run(project, config, functions.drop(1))
                }
              }

            }.recover {
              case ex: Throwable => log.completed(format(f, ex.getMessage), Some(ex))
            }
          }
        }
      }
    }
  }

}
