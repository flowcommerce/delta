package db

import io.flow.delta.v0.models.{Project, Scms, Visibility}
import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ProjectsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val projectsWriteDao = app.injector.instanceOf[ProjectsWriteDao]

  lazy val org = createOrganization()
  lazy val project1 = createProject(org)
  lazy val project2 = createProject(org)

  "findByOrganizationIdAndName" in {
    ProjectsDao.findByOrganizationIdAndName(Authorization.All, org.id, project1.name).map(_.id) must be(
      Some(project1.id)
    )

    ProjectsDao.findByOrganizationIdAndName(Authorization.All, createTestKey(), project1.name) must be(None)
    ProjectsDao.findByOrganizationIdAndName(Authorization.All, org.id, createTestName()) must be(None)
  }

  "findById" in {
    ProjectsDao.findById(Authorization.All, project1.id).map(_.id) must be(
      Some(project1.id)
    )

    ProjectsDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "delete" in {
    val project = createProject()
    projectsWriteDao.delete(systemUser, project)
    ProjectsDao.findById(Authorization.All, project.id) must be(None)
  }

  "update" in {
    val form = createProjectForm(org)
    val project = createProject(org)(form)
    projectsWriteDao.update(systemUser, project, form.copy(uri = "http://github.com/mbryzek/test"))
    ProjectsDao.findById(Authorization.All, project.id).map(_.uri) must be(Some("http://github.com/mbryzek/test"))
  }

  "update allows name change" in {
    val form = createProjectForm(org)
    val project = createProject(org)(form)
    val newName = project.name + "2"
    val updated = projectsWriteDao.update(systemUser, project, form.copy(name = newName)).right.get
    updated.id must be(project.id)
    updated.name must be(newName)
  }

  "validates" must {
    "SCMS" in {
      val form = createProjectForm(org).copy(scms = Scms.UNDEFINED("other"))
      projectsWriteDao.create(systemUser, form) must be(Left(Seq("Scms not found")))
    }

    "SCMS URI" in {
      val form = createProjectForm(org).copy(scms = Scms.Github, uri = "http://github.com/mbryzek")
      projectsWriteDao.create(systemUser, form) must be(
        Left(Seq("Invalid uri path[http://github.com/mbryzek] missing project name"))
      )
    }

    "empty name" in {
      val form = createProjectForm(org).copy(name = "   ")
      projectsWriteDao.create(systemUser, form) must be(Left(Seq("Name cannot be empty")))
    }

    "duplicate names" in {
      val project = createProject(org)
      val form = createProjectForm(org).copy(name = project.name.toString.toUpperCase)
      projectsWriteDao.create(systemUser, form) must be(Left(Seq("Project with this name already exists")))
      projectsWriteDao.validate(systemUser, form, existing = Some(project)) must be(Nil)

      val org2 = createOrganization()
      val form2 = createProjectForm(org2).copy(name = project.name)
      projectsWriteDao.validate(systemUser, form2) must be(Nil)
    }

    "empty uri" in {
      val form = createProjectForm(org).copy(uri = "   ")
      projectsWriteDao.create(systemUser, form) must be(Left(Seq("Uri cannot be empty")))
    }

  }

  "findAll" must {

    "ids" in {
      ProjectsDao.findAll(Authorization.All, ids = Some(Seq(project1.id, project2.id))).map(_.id).sorted must be(
        Seq(project1.id, project2.id).sorted
      )

      ProjectsDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
      ProjectsDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
      ProjectsDao.findAll(Authorization.All, ids = Some(Seq(project1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(project1.id))
    }

    "name" in {
      ProjectsDao.findAll(Authorization.All, name = Some(project1.name.toUpperCase)).map(_.id) must be(
        Seq(project1.id)
      )

      ProjectsDao.findAll(Authorization.All, name = Some(UUID.randomUUID.toString)).map(_.id) must be(Nil)
    }

    "organizationId" in {
      ProjectsDao.findAll(Authorization.All, id = Some(project1.id), organizationId = Some(org.id)).map(_.id) must be(
        Seq(project1.id)
      )

      ProjectsDao.findAll(Authorization.All, id = Some(project1.id), organizationId = Some(createOrganization().id)) must be(Nil)
    }

    "minutesSinceLastEvent" in {
      createEvent(project1)

      ProjectsDao.findAll(Authorization.All, id = Some(project1.id), minutesSinceLastEvent = Some(-100)).map(_.id) must be(
        Seq(project1.id)
      )

      ProjectsDao.findAll(Authorization.All, id = Some(project1.id), minutesSinceLastEvent = Some(100)) must be(Nil)
    }

    "authorization for public projects" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Public))

      ProjectsDao.findAll(Authorization.PublicOnly, id = Some(project.id)).map(_.id) must be(Seq(project.id))
      ProjectsDao.findAll(Authorization.All, id = Some(project.id)).map(_.id) must be(Seq(project.id))
      ProjectsDao.findAll(Authorization.Organization(org.id), id = Some(project.id)).map(_.id) must be(Seq(project.id))
      ProjectsDao.findAll(Authorization.Organization(createOrganization().id), id = Some(project.id)).map(_.id) must be(Seq(project.id))
      ProjectsDao.findAll(Authorization.User(user.id), id = Some(project.id)).map(_.id) must be(Seq(project.id))
    }

    "authorization for private projects" in {
      val user = createUser()
      val org = createOrganization(user = user)
      val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Private))

      ProjectsDao.findAll(Authorization.PublicOnly, id = Some(project.id)) must be(Nil)
      ProjectsDao.findAll(Authorization.All, id = Some(project.id)).map(_.id) must be(Seq(project.id))
      ProjectsDao.findAll(Authorization.Organization(org.id), id = Some(project.id)).map(_.id) must be(Seq(project.id))
      ProjectsDao.findAll(Authorization.Organization(createOrganization().id), id = Some(project.id)) must be(Nil)
      ProjectsDao.findAll(Authorization.User(user.id), id = Some(project.id)).map(_.id) must be(Seq(project.id))
      ProjectsDao.findAll(Authorization.User(createUser().id), id = Some(project.id)) must be(Nil)
    }

  }

}


