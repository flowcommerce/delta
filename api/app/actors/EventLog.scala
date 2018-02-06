package io.flow.delta.actors

import io.flow.delta.v0.models.Project

trait EventLog {

  /**
    * The prefix used in entries in the log. By default this is the
    * class name (exluding its package)
    */
  def logPrefix: String = {
    val base = format(this)
    withProject { project =>
      base + s"[${project.id}]"
    }.getOrElse(base)
  }

  /**
    * Prepend the description with the class name of the
    * function. This lets us have automatic messages like
    * "TagMaster: xxx"
    */
  def format(f: Any, desc: String): String = {
    format(f) + ": " + desc
  }

  def format(f: Any): String = {
    val name = f.getClass.getName
    val idx = name.lastIndexOf(".")  // Remove classpath to just get function name
    val className = name.substring(idx + 1)
    className.endsWith("$") match {
      case true => className.dropRight(1) // Remove trailing $
      case false => className
    }
  }
  
  /**
    * Event log relies on a project; this method can be provided by mixing in WithProject
    **/
  def withProject[T](f: Project => T): Option[T]

  def log(projectId: String): io.flow.delta.api.lib.EventLog = io.flow.delta.api.lib.EventLog.withSystemUser(projectId, logPrefix)

}
