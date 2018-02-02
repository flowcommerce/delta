package db

import java.util.UUID

import io.flow.delta.v0.models.{OrganizationSummary, ProjectSummary, Visibility}
import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec

class ItemsDaoSpec extends FlowPlaySpec with Helpers {

  lazy val org = createOrganization()

  "replace" in {
    val form = createItemForm(org)()
    val item1 = itemsDao.replace(systemUser, form)

    val item2 = itemsDao.replace(systemUser, form)
    item1.id must not be(item2.id)
    item1.label must be(item2.label)
  }

  "findById" in {
    val item = replaceItem(org)()
    itemsDao.findById(Authorization.All, item.id).map(_.id) must be(
      Some(item.id)
    )

    itemsDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findByObjectId" in {
    val project = createProject(org)()
    val item = itemsDao.replaceProject(systemUser, project)
    itemsDao.findByObjectId(Authorization.All, project.id).map(_.id) must be(
      Some(item.id)
    )

    itemsDao.findByObjectId(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findAll by ids" in {
    val item1 = replaceItem(org)()
    val item2 = replaceItem(org)()

    itemsDao.findAll(Authorization.All, ids = Some(Seq(item1.id, item2.id))).map(_.id).sorted must be(
      Seq(item1.id, item2.id).sorted
    )

    itemsDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
    itemsDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    itemsDao.findAll(Authorization.All, ids = Some(Seq(item1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(item1.id))
  }

  "supports projects" in {
    val project = createProject(org)

    val itemProject = itemsDao.replaceProject(systemUser, project)
    val actual = itemsDao.findByObjectId(Authorization.All, project.id).getOrElse {
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

    itemsDao.findAll(Authorization.All, q = Some(project.id.toString)).headOption.map(_.id) must be(Some(actual.id))
    itemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
  }

  "authorization for public projects" in {
    val user = createUserReference()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Public))
    val item = itemsDao.replaceProject(systemUser, project)

    itemsDao.findAll(Authorization.PublicOnly, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    itemsDao.findAll(Authorization.All, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    itemsDao.findAll(Authorization.Organization(org.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    itemsDao.findAll(Authorization.Organization(createOrganization().id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    itemsDao.findAll(Authorization.User(user.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
  }

  "authorization for private projects" in {
    val user = createUserReference()
    val org = createOrganization(user = user)
    val project = createProject(org)(createProjectForm(org).copy(visibility = Visibility.Private))
    val item = itemsDao.replaceProject(systemUser, project)

    itemsDao.findAll(Authorization.PublicOnly, objectId = Some(project.id)) must be(Nil)
    itemsDao.findAll(Authorization.All, objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    itemsDao.findAll(Authorization.Organization(org.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    itemsDao.findAll(Authorization.Organization(createOrganization().id), objectId = Some(project.id)) must be(Nil)
    itemsDao.findAll(Authorization.User(user.id), objectId = Some(project.id)).map(_.id) must be(Seq(item.id))
    itemsDao.findAll(Authorization.User(createUser().id), objectId = Some(project.id)) must be(Nil)
  }

}

