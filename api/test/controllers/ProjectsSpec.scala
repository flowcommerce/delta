package controllers

import java.util.UUID

import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.{Organization, Project, Visibility}

class ProjectsSpec extends MockClient with db.Helpers {

  private[this] lazy val org: Organization = createOrganization()
  private[this] lazy val project1: Project = createProject(org)()
  private[this] lazy val project2: Project = createProject(org)()

  "GET /projects by id" in {
    await(
      identifiedClientSystemUser().projects.get(id = Some(Seq(project1.id)))
    ).map(_.id) must be(
      Seq(project1.id)
    )

    await(
      identifiedClientSystemUser().projects.get(id = Some(Seq(UUID.randomUUID.toString)))
    ).map(_.id) must be(
      Nil
    )
  }

  "GET /projects by name" in {
    await(
      identifiedClientSystemUser().projects.get(name = Some(project1.name))
    ).map(_.name) must be(
      Seq(project1.name)
    )

    await(
      identifiedClientSystemUser().projects.get(name = Some(project1.name.toUpperCase))
    ).map(_.name) must be(
      Seq(project1.name)
    )

    await(
      identifiedClientSystemUser().projects.get(name = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /projects/:id" in {
    await(identifiedClientSystemUser().projects.getById(project1.id)).id must be(project1.id)
    await(identifiedClientSystemUser().projects.getById(project2.id)).id must be(project2.id)

    expectNotFound {
      identifiedClientSystemUser().projects.getById(UUID.randomUUID.toString)
    }
  }

  "POST /projects" in {
    val form = createProjectForm(org)
    val project = await(identifiedClientSystemUser().projects.post(form))
    project.name must be(form.name)
    project.scms must be(form.scms)
    project.uri must be(form.uri)
  }

  "POST /projects validates duplicate name" in {
    expectErrors(
      identifiedClientSystemUser().projects.post(createProjectForm(org).copy(name = project1.name))
    ).genericError.messages must be(
      Seq("Project with this name already exists")
    )
  }

  "PUT /projects/:id" in {
    val form = createProjectForm(org)
    val project = createProject(org)(form)
    val newUri = "http://github.com/mbryzek/test"
    await(identifiedClientSystemUser().projects.putById(project.id, form.copy(uri = newUri)))
    await(identifiedClientSystemUser().projects.getById(project.id)).uri must be(newUri)
  }

  "DELETE /projects" in {
    val project = createProject(org)()
    await(
      identifiedClientSystemUser().projects.deleteById(project.id)
    ) must be(())

    expectNotFound(
      identifiedClientSystemUser().projects.getById(project.id)
    )

    expectNotFound(
      identifiedClientSystemUser().projects.deleteById(project.id)
    )
  }

  "DELETE /projects validates membership" in {
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Public))

    val user2 = createUser()
    expectNotAuthorized(
      identifiedClientForUser(UserReference(user2.id)).projects.deleteById(project.id)
    )

    await(
      identifiedClientSystemUser().projects.getById(project.id)
    ).id must be (project.id)
  }

}
