package io.flow.delta.api.lib

import io.flow.delta.v0.models.Version

/**
  * Represents a difference in the number of instances of a single version of a project.
  */
case class StateDiff(versionName: String, lastInstances: Long, desiredInstances: Long)

object StateDiff {

  /**
    * Compares last to desired, returning a list of StateDiff
    * records wherever an instance needs to be brought up.
    */
  def up(last: Seq[Version], desired: Seq[Version]): Seq[StateDiff] = {
    diff(last, desired).filter { d => d.desiredInstances > d.lastInstances }
  }

  /**
    * Compares last to desired, returning a list of StateDiff
    * records wherever an instance needs to be brought down.
    */
  def down(last: Seq[Version], desired: Seq[Version]): Seq[StateDiff] = {
    diff(last, desired).filter { d => d.desiredInstances < d.lastInstances }
  }

  /**
    * Compares last state to desired state, returning a list of
    * StateDiff objects for any differences. Excludes any versions
    * where last state is the desired state - e.g. if last ==
    * desired, you will get back Nil.
    */
  def diff(last: Seq[Version], desired: Seq[Version]): Seq[StateDiff] = {
    desired.flatMap { expVersion =>
      val lastInstances: Long = last.find { _.name == expVersion.name }.map(_.instances).getOrElse(0)
      lastInstances == expVersion.instances match {
        case true => {
          // Desired number of instances matches last. Nothing to do
          None
        }
        case false => {
          Some(StateDiff(expVersion.name, lastInstances, expVersion.instances))
        }
      }
    } ++ last.flatMap { lastVersion =>
      desired.find { _.name == lastVersion.name } match {
        case None => {
          // A version in last that is not in desired
          Some(StateDiff(lastVersion.name, lastVersion.instances, 0))
        }
        case Some(_) => {
          // Already picked up in earlier loop
          None
        }
      }
    }
  }
  
}
