package io.flow.delta.actors

import io.flow.delta.v0.models.{Build, Project}
import io.flow.delta.config.v0.models.{BuildStage, ConfigProject, ProjectStage}
import play.api.Application

import scala.collection.mutable
import scala.concurrent.Future

sealed trait SupervisorResult {
  def merge(result: SupervisorResult): SupervisorResult
}

object SupervisorResult {
  case class Ready(description: String) extends SupervisorResult {
    override def merge(result: SupervisorResult): SupervisorResult = {
      result match {
        case Ready(desc) => SupervisorResult.Ready(Seq(description, desc).mkString(", "))
        case _ => sys.error("Cannot merge different result types")
      }
    }
  }

  case class Change(description: String) extends SupervisorResult {
    override def merge(result: SupervisorResult): SupervisorResult = {
      result match {
        case Change(desc) => SupervisorResult.Change(Seq(description, desc).mkString(", "))
        case _ => sys.error("Cannot merge different result types")
      }
    }
  }

  case class Checkpoint(description: String) extends SupervisorResult {
    override def merge(result: SupervisorResult): SupervisorResult = {
      result match {
        case Checkpoint(desc) => SupervisorResult.Checkpoint(Seq(description, desc).mkString(", "))
        case _ => sys.error("Cannot merge different result types")
      }
    }
  }

  case class Error(description: String, ex: Option[Throwable] = None) extends SupervisorResult {
    override def merge(result: SupervisorResult): SupervisorResult = {
      result match {
        case Error(desc, _) => SupervisorResult.Error(Seq(description, desc).mkString(", "), ex)
        case _ => sys.error("Cannot merge different result types")
      }
    }
  }

  private[this] val MergeOrder = Seq("change", "error", "checkpoint", "ready")

  def merge(results: Seq[SupervisorResult]): SupervisorResult = {
    val byType = mutable.Map[String, SupervisorResult]()

    results.foreach { r =>
      val name = r match {
        case Ready(desc) => "ready"
        case Change(desc) => "change"
        case Checkpoint(desc) => "checkpoint"
        case Error(desc, ex) => "error"
      }

      byType.get(name) match {
        case None => {
          byType += (name -> r)
        }

        case Some(existing) => {
          byType += (name -> existing.merge(r))
        }
      }
    }

    MergeOrder.flatMap { name =>
      byType.get(name)
    }.headOption.getOrElse {
      sys.error("Must have at least 1 result")
    }
  }
}

trait ProjectSupervisorFunction {

  /**
    * The stage that this function belongs to. Allows us to only run
    * this function when this stage is enabled
    */
  def stage: ProjectStage

  /**
   * Responsible for actually running this function
   */
  def run(
    project: Project,
    config: ConfigProject
  ) (
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult]

}

trait BuildSupervisorFunction {

  /**
    * The stage that this function belongs to. Allows us to only run
    * this function when this stage is enabled
    */
  def stage: BuildStage

  /**
   * Responsible for actually running this function
   */
  def run(
    build: Build
  ) (
    implicit ec: scala.concurrent.ExecutionContext,
    app: Application
  ): Future[SupervisorResult]

}

