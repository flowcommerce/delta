package io.flow.delta.api.lib

import db.{EventsDao, UsersDao}
import io.flow.delta.v0.models.{EventType, Project}
import io.flow.common.v0.models.UserReference
import java.io.{PrintWriter, StringWriter}
import org.joda.time.DateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object EventLog {

  def withSystemUser(
    project: Project,
    prefix: String
  ): EventLog = {
    EventLog(UsersDao.systemUser, project, prefix)
  }

}

case class EventLog(
  user: UserReference,
  project: Project,
  prefix: String
) {

  def changed(message: String) = {
    process(EventType.Change, message)
  }

  def skipped(message: String) = {
    process(EventType.Info, s"skipped $message")
  }

  def message(message: String) = {
    process(EventType.Info, message)
  }

  /**
    * Indicates a start event. Should be followed by a completed event
    * when the function is complete.
    */
  def started(message: String) = {
    process(EventType.Info, s"started $message")
  }

  /**
    * Indicates completion. If there was an error, include the
    * exception. If no exception, we assume successful completion.
    */
  def completed(message: String, error: Option[Throwable] = None) = {
    error match {
      case None => {
        process(EventType.Info, s"completed $message")
      }
      case Some(ex) => {
        process(EventType.Info, s"error $message", Some(ex))
      }
    }
  }

  /**
    * Logs an error
    */
  def error(message: String, error: Option[Throwable] = None) = {
    process(EventType.Info, s"error $message", error)
  }

  /**
    * Records a checkpoint - main purpose is to communicate that
    * info is being made. We intend to build functions that detect
    * failure based on no activity written to the log. So long running
    * functions should periodically checkpoint to track info in
    * the log.
    */
  def checkpoint(message: String) = {
    process(EventType.Info, s"checkpoint $message")
  }

  /**
    * Wraps the execution of a function with a started and completed
    * entry in the log. Catches and handles errors as well.
    */
  def runSync[T](
    message: String,
    quiet: Boolean = false
  ) (
    f: => T
  ) (
    implicit ec: ExecutionContext
  ): Future[T] = {
    runAsync(message, quiet) {
      Future { f }
    }
  }

  def runAsync[T](
    message: String,
    quiet: Boolean = false
  ) (
    f: => Future[T]
  ) (
    implicit ec: ExecutionContext
  ): Future[T] = {
    if (!quiet) {
      started(message)
    }

    f.map { result =>
      if (!quiet) {
        completed(message + ": " + result)
      }
      result
    }.recover {
      case ex: Throwable => {
        completed(message, Some(ex))
        throw ex
      }
    }
  }

  private[this] def process(typ: EventType, message: String, ex: Option[Throwable] = None) {
    val formatted = s"$prefix: $message"
    val ts = new DateTime()

    ex match {
      case None => {
        println(s"[$ts] ${project.id} $typ $formatted")
        EventsDao.create(user, project.id, typ, formatted, ex = None)
      }
      case Some(error) =>
        val sw = new StringWriter
        error.printStackTrace(new PrintWriter(sw))
        println(s"[$ts] ${project.id} error $formatted: ${error.getMessage}\n\n$sw")
        EventsDao.create(user, project.id, EventType.Info, s"error $message", ex = Some(error))
    }
  }

}
