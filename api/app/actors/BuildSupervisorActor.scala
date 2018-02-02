package io.flow.delta.actors

import akka.actor.Actor
import db.{BuildsDao, BuildDesiredStatesDao, ConfigsDao}
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.v0.models.{Build, Version}
import io.flow.delta.config.v0.{models => config}
import io.flow.play.actors.ErrorHandler
import io.flow.postgresql.Authorization
import play.libs.Akka

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
    functions.Scale
  )

}

class BuildSupervisorActor extends Actor with ErrorHandler with DataBuild with BuildEventLog {

  private[this] implicit val ec = Akka.system.dispatchers.lookup("supervisor-actor-context")
  override lazy val configsDao = play.api.Play.current.injector.instanceOf[ConfigsDao]

  def receive = {

    case msg @ BuildSupervisorActor.Messages.Data(id) => withErrorHandler(msg) {
      setBuildId(id)
    }

    case msg @ BuildSupervisorActor.Messages.PursueDesiredState => withErrorHandler(msg) {
      withEnabledBuild { build =>
        withBuildConfig { buildConfig =>
          log.runSync("PursueDesiredState") {
            run(build, buildConfig.stages, BuildSupervisorActor.Functions)
          }
        }
      }
    }

    /**
      * Indicates that something has happened for the tag with
      * specified name (e.g. 0.0.2). If this tag is in the build's
      * desired state (or ahead of the desired state), triggers
      * PursueDesiredState. Otherwise a no-op.
      */
    case msg @ BuildSupervisorActor.Messages.CheckTag(name) => withErrorHandler(msg) {  
      withEnabledBuild { build =>
        BuildDesiredStatesDao.findByBuildId(Authorization.All, build.id) match {
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
      
  }

  /**
    * Sequentially runs through the list of functions. If any of the
    * functions returns a SupervisorResult.Changed or
    * SupervisorResult.Error, returns that result. Otherwise will
    * return Ready at the end of all the functions.
    */
  private[this] def run(build: Build, stages: Seq[config.BuildStage], functions: Seq[BuildSupervisorFunction]) {
    functions.headOption match {
      case None => {
        SupervisorResult.Ready("All functions returned without modification")
      }
      case Some(f) => {
        stages.contains(f.stage) match {
          case false => {
            log.skipped(s"Stage ${f.stage} is disabled")
            run(build, stages, functions.drop(1))
          }
          case true => {
            log.started(format(f))
            f.run(build).map { result =>
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
                  run(build, stages, functions.drop(1))
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
