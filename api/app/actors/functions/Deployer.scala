package io.flow.delta.actors.functions

import io.flow.delta.actors.{MainActor, MainActorProvider, SupervisorResult}
import io.flow.delta.lib.{StateFormatter, Text}
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.v0.models.{Build, State, Version}
import org.joda.time.DateTime

case class Deployer(build: Build, last: State, desired: State) {

  /**
    * Scales up or down the build instances to move last state
    * towards desired state. This works as follows:
    * 
    *   1. Find any instances that need to be brought up, and issue
    *      messages to bring those instances up.
    * 
    *   2. If there are multiple versions in last state AND the
    *      version in desired state is not in last state, bring down
    *      all but the earliest version. This is to enable ECS to
    *      stabilize and release the latest version.
    *      See https://github.com/flowcommerce/delta/issues/253
    * 
    *   3. Only if ALL instances are UP, find instances that need
    *      to be brought down, and fire events to bring those down.
    * 
    * This ordering attempts to minimize the case that we bring down
    * the last instances which in turn leaves a service offline or
    * unhealthy.
    */
  def scale(): SupervisorResult = {
    extras().toList match {
      case Nil => scaleDiff()
      case versions => scaleDown(versions)
    }
  }

  private[this] def extras(): Seq[Version] = {
    val allVersions = last.versions ++ desired.versions

    Seq(
      last.versions.headOption.map(_.name),
      desired.versions.lastOption.map(_.name)
    ).flatten.distinct.toList match {
      case first :: last :: Nil => {
        allVersions.filter { v => v.name != first && v.name != last }
      }
      case _ => Nil
    }
  }

  private[this] def scaleDiff(): SupervisorResult = {
    StateDiff.up(last.versions, desired.versions).toList match {
      case Nil => {
        StateDiff.down(last.versions, desired.versions).toList match {
          case Nil => {
            SupervisorResult.Ready(
              s"Last state[%s] matches desired state[%s]".format(
                StateFormatter.label(last.versions),
                StateFormatter.label(desired.versions)
              )
            )
          }
          case diffs => {
            MainActorProvider.ref() ! MainActor.Messages.Scale(build.id, diffs)
            SupervisorResult.Change("Scale Down: " + toLabel(diffs))
          }
        }
      }
      case diffs => {
        MainActorProvider.ref() ! MainActor.Messages.Scale(build.id, diffs)
        SupervisorResult.Change("Scale Up: " + toLabel(diffs))
      }
    }
  }

  /**
    * Scales down all instances of the versions specified.
    */
  private[this] def scaleDown(versions: Seq[Version]): SupervisorResult = {
    assert(!versions.isEmpty, "Must have at least one version")
    val diffs = versions.map { v =>
      StateDiff(
        versionName = v.name,
        lastInstances = v.instances,
        desiredInstances = 0
      )
    }
    MainActorProvider.ref() ! MainActor.Messages.Scale(build.id, diffs)
    SupervisorResult.Change(s"Scale Down Extras: " + toLabel(diffs))
  }

  private[this] def toLabel(diffs: Seq[StateDiff]): String = {
    diffs.flatMap { diff =>
      if (diff.lastInstances > diff.desiredInstances) {
        val label = Text.pluralize(diff.lastInstances - diff.desiredInstances, "instance", "instances")
        Some(s"${diff.versionName}: Remove $label")
      } else if (diff.lastInstances < diff.desiredInstances) {
        val label = Text.pluralize(diff.desiredInstances - diff.lastInstances , "instance", "instances")
        Some(s"${diff.versionName}: Add $label")
      } else {
        None
      }
    }.mkString(", ")
  }
}
