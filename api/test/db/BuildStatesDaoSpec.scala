package db

import io.flow.delta.v0.models.{Build, State, StateForm, Version}
import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec

class BuildStatesDaoSpec extends FlowPlaySpec with Helpers {

  def upsertBuildDesiredState(
    build: Build = upsertBuild(),
    form: StateForm = createStateForm()
  ): State = {
    rightOrErrors(buildDesiredStatesDao.create(systemUser, build, form))
  }

  def createStateForm(): StateForm = {
    StateForm(
      versions = Seq(
        Version(name = "0.0.1", instances = 3),
        Version(name = "0.0.2", instances = 2)
      )
    )
  }

  "create desired" in {
    val build = upsertBuild()
    val state = rightOrErrors(buildDesiredStatesDao.create(systemUser, build, createStateForm()))
    state.versions.map(_.name) must be(Seq("0.0.1", "0.0.2"))
    state.versions.map(_.instances) must be(Seq(3, 2))
  }

  "create actual" in {
    val build = upsertBuild()
    val state = rightOrErrors(buildLastStatesDao.create(systemUser, build, createStateForm()))
    state.versions.map(_.name) must be(Seq("0.0.1", "0.0.2"))
    state.versions.map(_.instances) must be(Seq(3, 2))
  }

  "upsert" in {
    val build = upsertBuild()
    val state = rightOrErrors(buildDesiredStatesDao.upsert(systemUser, build, createStateForm()))
    val second = rightOrErrors(buildDesiredStatesDao.upsert(systemUser, build, createStateForm()))
    second.versions.map(_.name) must be(Seq("0.0.1", "0.0.2"))
    state.versions.map(_.instances) must be(Seq(3, 2))
  }

  "delete" in {
    val build = upsertBuild()
    val state = upsertBuildDesiredState(build)
    buildDesiredStatesDao.delete(systemUser, build)
    buildDesiredStatesDao.findByBuildId(Authorization.All, build.id) must be(None)
  }

  "saving prunes records w/ zero instances" in {
    val form = StateForm(
      versions = Seq(
        Version(name = "0.0.1", instances = 0),
        Version(name = "0.0.2", instances = 2)
      )
    )
    
    val build = upsertBuild()
    val state = rightOrErrors(buildDesiredStatesDao.create(systemUser, build, form))
    state.versions.map(_.name) must be(Seq("0.0.2"))
    state.versions.map(_.instances) must be(Seq(2))
  }

}
