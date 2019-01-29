package db

import java.util.UUID

import io.flow.delta.v0.models._
import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec

class OrganizationsDaoSpec extends FlowPlaySpec with Helpers {

  "create" in {
    val form = createOrganizationForm()
    val organization = organizationsWriteDao.create(systemUser, form).right.getOrElse {
      sys.error("Failed to create org")
    }
    organization.id must be(form.id)
    organization.docker must be(form.docker)
  }

  "update docker organization" in {
    val org = createOrganization()
    val form = OrganizationForm(
      id = org.id,
      docker = Docker(provider=DockerProvider.DockerHub, organization="updated"),
      travis = org.travis
    )

    val updated = organizationsWriteDao.update(systemUser, org, form).right.getOrElse {
      sys.error("Failed to update org")
    }
    updated.id must be(form.id)
    updated.docker.organization must be("updated")
    updated.travis.organization must be(org.travis.organization)
  }

  "update travis organization" in {
    val org = createOrganization()
    val form = OrganizationForm(
      id = org.id,
      docker = org.docker,
      travis = Travis(organization = "updated")
    )

    val updated = organizationsWriteDao.update(systemUser, org, form).right.getOrElse {
      sys.error("Failed to update org")
    }
    updated.id must be(form.id)
    updated.docker.organization must be(org.docker.organization)
    updated.travis.organization must be("updated")

  }

  "creation users added as admin of org" in {
    val user = createUserReference()
    val form = createOrganizationForm()
    val org = organizationsWriteDao.create(user, form).right.getOrElse {
      sys.error("Failed to create org")
    }
    val membership = membershipsDao.findByOrganizationIdAndUserId(Authorization.All, org.id, user.id).getOrElse {
      sys.error("Failed to create membership record")
    }
    membership.role must be(Role.Admin)
  }

  "delete" in {
    val org = createOrganization()
    organizationsWriteDao.delete(systemUser, org)
    organizationsDao.findById(Authorization.All, org.id) must be(None)
  }

  "findById" in {
    val organization = createOrganization()
    organizationsDao.findById(Authorization.All, organization.id).map(_.id) must be(
      Some(organization.id)
    )

    organizationsDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findAll by ids" in {
    val organization1 = createOrganization()
    val organization2 = createOrganization()

    organizationsDao.findAll(Authorization.All, ids = Some(Seq(organization1.id, organization2.id)), limit = None).map(_.id).sorted must be(
      Seq(organization1.id, organization2.id).sorted
    )

    organizationsDao.findAll(Authorization.All, ids = Some(Nil), limit = None) must be(Nil)
    organizationsDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString)), limit = None) must be(Nil)
    organizationsDao.findAll(Authorization.All, ids = Some(Seq(organization1.id, UUID.randomUUID.toString)), limit = None).map(_.id) must be(Seq(organization1.id))
  }

  "validate" must {

    "keep key url friendly" in {
      organizationsWriteDao.validate(createOrganizationForm().copy(id = "flow commerce")) must be(
        Seq("Id must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid id would be: flow-commerce")
      )
    }

    "requires valid docker provider" in {
      val form = createOrganizationForm()
      organizationsWriteDao.validate(form.copy(docker = Docker(provider = DockerProvider.UNDEFINED("other"), organization="flow"))) must be(
        Seq("Docker provider not found")
      )
    }

    "requires docker organization" in {
      val form = createOrganizationForm()
      organizationsWriteDao.validate(form.copy(docker = Docker(provider = DockerProvider.DockerHub, organization=" "))) must be(
        Seq("Docker organization is required")
      )
    }

  }

  "authorization for organizations" in {
    val user = createUserReference()
    val org = createOrganization(user = user)

    organizationsDao.findAll(Authorization.PublicOnly, id = Some(org.id), limit = None) must be(Nil)
    organizationsDao.findAll(Authorization.All, id = Some(org.id), limit = None).map(_.id) must be(Seq(org.id))
    organizationsDao.findAll(Authorization.Organization(org.id), id = Some(org.id), limit = None).map(_.id) must be(Seq(org.id))
    organizationsDao.findAll(Authorization.Organization(createOrganization().id), id = Some(org.id), limit = None) must be(Nil)
    organizationsDao.findAll(Authorization.User(user.id), id = Some(org.id), limit = None).map(_.id) must be(Seq(org.id))
    organizationsDao.findAll(Authorization.User(createUser().id), id = Some(org.id), limit = None) must be(Nil)
  }

}
