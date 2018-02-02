package db

import io.flow.delta.v0.models.{OrganizationSummary, ProjectSummary, Visibility}
import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ItemsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "replace" in {
    val form = createItemForm(org)()
    val item1 = ItemsDao.replace(systemUser, form)

    val item2 = ItemsDao.replace(systemUser, form)
    item1.id must not be(item2.id)
    item1.label must be(item2.label)
  }

  "findById" in {
    val item = replaceItem(org)()
    ItemsDao.findById(Authorization.All, item.id).map(_.id) must be(
      Some(item.id)
    )

    ItemsDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findByObjectId" in {
    val project = createProject(org)()
    val item = ItemsDao.replaceProject(systemUser, project)
    ItemsDao.findByObjectId(Authorization.All, project.id).map(_.id) must be(
      Some(item.id)
    )

    ItemsDao.findByObjectId(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findAll by ids" in {
    val item1 = replaceItem(org)()
    val item2 = replaceItem(org)()

    ItemsDao.findAll(Authorization.All, ids = Some(Seq(item1.id, item2.id))).map(_.id).sorted must be(
      Seq(item1.id, item2.id).sorted
    )

    ItemsDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
    ItemsDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    ItemsDao.findAll(Authorization.All, ids = Some(Seq(item1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(item1.id))
  }

  "supports projects" in {
    val project = createProject(org)

    val itemProject = ItemsDao.replaceProject(systemUser, project)
    val actual = ItemsDao.findByObjectId(Authorization.All, project.id).getOrElse {
      sys.error("Failed to create project")
    }
    actual.label must be(project.name)
    actual.summary must be(
      ProjectSummary(
        id = project.id,
        organization = OrganizationSummary(org.id),
        name = project.name,
        uri = project.uri
      )
    )

    ItemsDao.findAll(Authorization.All, q = Some(project.id.toString)).headOption.map(_.id) must be(Some(actual.id))
    ItemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "authorization for public projects" in {
    val user = createUser()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Public))
    val item = ItemsDao.replaceProject(systemUser, project)

    ItemsDao.findAll(Authorization.PublicOnly, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.All, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(org.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(createOrganization().id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.User(user.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
  }

  "authorization for private projects" in {
    val user = createUser()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Private))
    val item = ItemsDao.replaceProject(systemUser, project)

    ItemsDao.findAll(Authorization.PublicOnly, objectId = Some(project.id)) must be(Nil)
    ItemsDao.findAll(Authorization.All, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(org.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.Organization(createOrganization().id), objectId = Some(project.id)) must be(Nil)
    ItemsDao.findAll(Authorization.User(user.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    ItemsDao.findAll(Authorization.User(createUser().id), objectId = Some(project.id)) must be(Nil)
  }

}

