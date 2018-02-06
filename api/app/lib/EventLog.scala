package io.flow.delta.api.lib

import java.io.{PrintWriter, StringWriter}
import javax.inject.Inject

import db.EventsDao
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.EventType
import io.flow.play.util.Constants
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}


case class EventLog (
  user: UserReference,
  projectId: String,
  prefix: String
)

object EventLog {

  def withSystemUser(
    projectId: String,
    prefix: String
  ): EventLog = {
    EventLog(Constants.SystemUser, projectId, prefix)
  }

}

class EventLogProcessor @Inject()(
  eventsDao: EventsDao
) {
  def changed(message: String, log: EventLog) = {
    process(EventType.Change, message, log = log)
  }

  def skipped(message: String, log: EventLog) = {
    process(EventType.Info, s"skipped $message", log = log)
  }

  def message(message: String, log: EventLog) = {
    process(EventType.Info, message, log = log)
  }

  /**
    * Indicates a start event. Should be followed by a completed event
    * when the function is complete.
    */
  def started(message: String, log: EventLog) = {
    process(EventType.Info, s"started $message", log = log)
  }

  /**
    * Indicates completion. If there was an error, include the
    * exception. If no exception, we assume successful completion.
    */
  def completed(message: String, error: Option[Throwable] = None, log: EventLog) = {
    error match {
      case None => {
        process(EventType.Info, s"completed $message", log = log)
      }
      case Some(ex) => {
        process(EventType.Info, s"error $message", Some(ex), log = log)
      }
    }
  }

  /**
    * Logs an error
    */
  def error(message: String, error: Option[Throwable] = None, log: EventLog) = {
    process(EventType.Info, s"error $message", error, log = log)
  }

  /**
    * Records a checkpoint - main purpose is to communicate that
    * info is being made. We intend to build functions that detect
    * failure based on no activity written to the log. So long running
    * functions should periodically checkpoint to track info in
    * the log.
    */
  def checkpoint(message: String, log: EventLog) = {
    process(EventType.Info, s"checkpoint $message", log = log)
  }

  /**
    * Wraps the execution of a function with a started and completed
    * entry in the log. Catches and handles errors as well.
    */
  def runSync[T](
    message: String,
    quiet: Boolean = false,
    log: EventLog
  ) (
    f: => T
  ) (
    implicit ec: ExecutionContext
  ): Future[T] = {
    runAsync(message, quiet, log = log) {
      Future { f }
    }
  }

  def runAsync[T](
    message: String,
    quiet: Boolean = false,
    log: EventLog
  ) (
    f: => Future[T]
  ) (
    implicit ec: ExecutionContext
  ): Future[T] = {
    if (!quiet) {
      started(message, log = log)
    }

    f.map { result =>
      if (!quiet) {
        completed(message + ": " + result, log = log)
      }
      result
    }.recover {
      case ex: Throwable => {
        completed(message, Some(ex), log = log)
        throw ex
      }
    }
  }

  private[this] def process(typ: EventType, message: String, ex: Option[Throwable] = None, log: EventLog) {
    val formatted = s"${log.prefix}: $message"
    val ts = new DateTime()

    ex match {
      case None => {
        println(s"[$ts] ${log.projectId} $typ $formatted")
        eventsDao.create(log.user, log.projectId, typ, formatted, ex = None)
      }
      case Some(error) =>
        val sw = new StringWriter
        error.printStackTrace(new PrintWriter(sw))
        println(s"[$ts] ${log.projectId} error $formatted: ${error.getMessage}\n\n$sw")
        eventsDao.create(log.user, log.projectId, EventType.Info, s"error $message", ex = Some(error))
    }
  }

}
