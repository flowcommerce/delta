package io.flow.delta.actors

import db.BuildsDao
import io.flow.delta.config.v0.{models => config}
import io.flow.delta.v0.models.{Build, Status}
import io.flow.log.RollbarLogger
import io.flow.postgresql.Authorization


trait DataBuild extends DataProject {

  val logger: RollbarLogger

  def buildsDao: BuildsDao

  private[this] var dataBuild: Option[Build] = None

  /**
    * Looks up the build with the specified ID, setting the local
    * dataBuild var to that build
    */
  def setBuildId(id: String) {
    buildsDao.findById(Authorization.All, id) match {
      case None => {
        dataBuild = None
        logger.withKeyValue("build_id", id).warn(s"Could not find build")
      }
      case Some(b) => {
        setProjectId(b.project.id)
        dataBuild = Some(b)
      }
    }
  }

  /**
    * Invokes the specified function w/ the current build
    */
  def withBuild[T](f: Build => T): Option[T] = {
    dataBuild.map { f(_) }
  }

  /**
    * Invokes the specified function w/ the current build, but only
    * if we have a build set.
    */
  def withEnabledBuild[T](f: Build => T): Option[T] = {
    dataBuild.flatMap { build =>
      build.status match {
        case Status.Enabled => Some(f(build))
        case Status.Paused | Status.UNDEFINED(_) => None
      }
    }
  }

  /**
    * Invokes the specified function w/ the current build config, but
    * only if we have an enabled configuration matching this build.
    */
  def withBuildConfig[T](f: config.Build => T): Option[T] = {
    dataBuild match {
      case None => {
        None
      }

      case Some(build) => {
        withConfig { config =>
          val bc = config.builds.find(_.name == build.name).getOrElse {
            sys.error(s"Build[${build.id}] does not have a configuration matching name[${build.name}]")
          }
          f(bc)
        }
      }
    }
  }

}