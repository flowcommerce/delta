package db

import io.flow.delta.v0.models.{Build, State, StateForm, Version}
import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class BuildStatesDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def upsertBuildDesiredState(
    build: Build = upsertBuild(),
    form: StateForm = createStateForm()
  ): State = {
    rightOrErrors(buildDesiredStatesWriteDao.create(systemUser, build, form))
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
    val state = rightOrErrors(buildDesiredStatesWriteDao.create(systemUser, build, createStateForm()))
    state.versions.map(_.name) must be(Seq("0.0.1", "0.0.2"))
    state.versions.map(_.instances) must be(Seq(3, 2))
  }

  "create actual" in {
    val build = upsertBuild()
    val state = rightOrErrors(buildLastStatesWriteDao.create(systemUser, build, createStateForm()))
    state.versions.map(_.name) must be(Seq("0.0.1", "0.0.2"))
    state.versions.map(_.instances) must be(Seq(3, 2))
  }

  "upsert" in {
    val build = upsertBuild()
    val state = rightOrErrors(buildDesiredStatesWriteDao.upsert(systemUser, build, createStateForm()))
    val second = rightOrErrors(buildDesiredStatesWriteDao.upsert(systemUser, build, createStateForm()))
    second.versions.map(_.name) must be(Seq("0.0.1", "0.0.2"))
    state.versions.map(_.instances) must be(Seq(3, 2))
  }

  "delete" in {
    val build = upsertBuild()
    val state = upsertBuildDesiredState(build)
    buildDesiredStatesWriteDao.delete(systemUser, build)
    BuildDesiredStatesDao.findByBuildId(Authorization.All, build.id) must be(None)
  }

  "saving prunes records w/ zero instances" in {
    val form = StateForm(
      versions = Seq(
        Version(name = "0.0.1", instances = 0),
        Version(name = "0.0.2", instances = 2)
      )
    )
    
    val build = upsertBuild()
    val state = rightOrErrors(buildDesiredStatesWriteDao.create(systemUser, build, form))
    state.versions.map(_.name) must be(Seq("0.0.2"))
    state.versions.map(_.instances) must be(Seq(2))
  }

}
