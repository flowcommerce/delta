package controllers

import io.flow.delta.v0.{Authorization, Client}
import io.flow.delta.v0.models.ProjectForm

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class ProjectsSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val project1 = createProject(org)()
  lazy val project2 = createProject(org)()

  "GET /projects by id" in new WithServer(port=port) {
    await(
      client.projects.get(id = Some(Seq(project1.id)))
    ).map(_.id) must beEqualTo(
      Seq(project1.id)
    )

    await(
      client.projects.get(id = Some(Seq(UUID.randomUUID.toString)))
    ).map(_.id) must be(
      Nil
    )
  }

  "GET /projects by name" in new WithServer(port=port) {
    await(
      client.projects.get(name = Some(project1.name))
    ).map(_.name) must beEqualTo(
      Seq(project1.name)
    )

    await(
      client.projects.get(name = Some(project1.name.toUpperCase))
    ).map(_.name) must beEqualTo(
      Seq(project1.name)
    )

    await(
      client.projects.get(name = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /projects/:id" in new WithServer(port=port) {
    await(client.projects.getById(project1.id)).id must beEqualTo(project1.id)
    await(client.projects.getById(project2.id)).id must beEqualTo(project2.id)

    expectNotFound {
      client.projects.getById(UUID.randomUUID.toString)
    }
  }

  "POST /projects" in new WithServer(port=port) {
    val form = createProjectForm(org)
    val project = await(client.projects.post(form))
    project.name must beEqualTo(form.name)
    project.scms must beEqualTo(form.scms)
    project.uri must beEqualTo(form.uri)
  }

  "POST /projects validates duplicate name" in new WithServer(port=port) {
    expectErrors(
      client.projects.post(createProjectForm(org).copy(name = project1.name))
    ).errors.map(_.message) must beEqualTo(
      Seq("Project with this name already exists")
    )
  }

  "PUT /projects/:id" in new WithServer(port=port) {
    val form = createProjectForm(org)
    val project = createProject(org)(form)
    val newUri = "http://github.com/mbryzek/test"
    await(client.projects.putById(project.id, form.copy(uri = newUri)))
    await(client.projects.getById(project.id)).uri must beEqualTo(newUri)
  }

  "DELETE /projects" in new WithServer(port=port) {
    val project = createProject(org)()
    await(
      client.projects.deleteById(project.id)
    ) must beEqualTo(())

    expectNotFound(
      client.projects.getById(project.id)
    )

    expectNotFound(
      client.projects.deleteById(project.id)
    )
  }

}
