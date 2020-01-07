package io.flow.delta.actors

import db.BuildsDao
import io.flow.delta.config.v0.models._
import io.flow.delta.lib.BuildNames
import io.flow.delta.v0.models.{Build, Status}
import io.flow.postgresql.Authorization
import lib.BuildConfigUtil

import scala.concurrent.duration.{FiniteDuration, MINUTES}
import scala.concurrent.{Await, Future}

trait DataBuild extends DataProject with EventLog {

  def buildsDao: BuildsDao

  private[this] var dataBuild: Option[Build] = None

  /**
    * Looks up the build with the specified ID, setting the local
    * dataBuild var to that build
    */
  def setBuildId(id: String): Unit = {
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

  override def logPrefix: String = {
    val base = format(this)
    dataBuild match {
      case None => base
      case Some(build) =>s"$base[${BuildNames.projectName(build)}]"
    }
  }

  /**
    * Invokes the specified function w/ the current build
    */
  def withBuild[T](f: Build => T): Unit = {
    dataBuild.foreach(f)
  }

  /**
    * Invokes the specified function w/ the current build, but only
    * if we have a build set.
    */
  def withEnabledBuild[T](f: Build => T): Unit = {
    dataBuild.foreach { build =>
      build.status match {
        case Status.Enabled =>
          f(build)
        case Status.Paused | Status.UNDEFINED(_) =>
      }
      ()
    }
  }

  def requiredBuildConfig: BuildConfig = {
    optionalBuildConfig.getOrElse {
      sys.error("No build config")
    }
  }

  def requiredEcsBuildConfig: EcsBuildConfig = {
    requiredBuildConfig match {
      case c: EcsBuildConfig => c
      case _: K8sBuildConfig | BuildConfigUndefinedType(_) => {
        sys.error("Must have an ecs build config")
      }
    }
  }

  private[this] def optionalBuildConfig: Option[BuildConfig] = {
    dataBuild match {
      case None => {
        None
      }

      case Some(build) => {
        withConfig { config =>
          BuildConfigUtil.findBuildByName(config.builds, build.name).getOrElse {
            sys.error(s"Build[${build.id}] does not have a configuration matching name[${build.name}]")
          }
        }
      }
    }
  }

  /**
    * Invokes the specified function w/ the current build config, but
    * only if we have an enabled configuration matching this build.
    */
  def withBuildConfig[T](f: BuildConfig => T): Option[T] = {
    optionalBuildConfig.map(f)
  }

  def withEcsBuildConfig[T](f: EcsBuildConfig => T): Option[T] = {
    optionalBuildConfig.flatMap {
      case c: EcsBuildConfig => Some(c)
      case _: K8sBuildConfig | _: BuildConfigUndefinedType => None
    }.map(f)
  }

  def withK8sBuildConfig[T](f: K8sBuildConfig => T): Option[T] = {
    optionalBuildConfig.flatMap {
      case c: K8sBuildConfig => Some(c)
      case _: EcsBuildConfig | _: BuildConfigUndefinedType => None
    }.map(f)
  }

  protected def await[T](f: => Future[T]): T = {
    Await.result(f, FiniteDuration(1, MINUTES))
  }

}
