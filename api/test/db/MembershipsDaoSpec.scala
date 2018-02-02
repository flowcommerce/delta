package db

import io.flow.delta.v0.models.{Membership, OrganizationSummary, Role}
import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class MembershipsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val user = createUser()
  lazy val membership = createMembership(createMembershipForm(org = org, user = user))

  "isMember by id" in {
    println("Creating membership")
    membership // Create the membership record
    println("membership: " + membership)

    MembershipsDao.isMember(org.id, user) must be(true)
    MembershipsDao.isMember(org.id, createUser()) must be(false)
    MembershipsDao.isMember(createOrganization().id, user) must be(false)
  }

  "findByOrganizationIdAndUserId" in {
    membership // Create the membership record

    MembershipsDao.findByOrganizationIdAndUserId(Authorization.All, org.id, user.id).map(_.id) must be(
      Some(membership.id)
    )

    MembershipsDao.findByOrganizationIdAndUserId(Authorization.All, UUID.randomUUID.toString, user.id) must be(None)
    MembershipsDao.findByOrganizationIdAndUserId(Authorization.All, org.id, UUID.randomUUID.toString) must be(None)
  }

  "findById" in {
    MembershipsDao.findById(Authorization.All, membership.id).map(_.id) must be(
      Some(membership.id)
    )

    MembershipsDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "soft delete" in {
    val membership = createMembership()
    MembershipsDao.delete(systemUser, membership)
    MembershipsDao.findById(Authorization.All, membership.id) must be(None)
  }

  "validates role" in {
    val form = createMembershipForm(role = Role.UNDEFINED("other"))
    MembershipsDao.validate(systemUser, form) must be(Seq("Invalid role. Must be one of: member, admin"))
  }

  "validates duplicate" in {
    val org = createOrganization()
    val user = createUser()
    val form = createMembershipForm(org = org, user = user, role = Role.Member)
    val membership = createMembership(form)

    MembershipsDao.validate(systemUser, form) must be(Seq("User is already a member"))
    MembershipsDao.validate(systemUser, form.copy(role = Role.Admin)) must be(Seq("User is already a member"))
  }

  "validates access to org" in {
    MembershipsDao.validate(createUser(), createMembershipForm()) must be(
      Seq("Organization does not exist or you are not authorized to access this organization")
    )
  }

  "findAll" must {

    "ids" in {
      val membership2 = createMembership()

      MembershipsDao.findAll(Authorization.All, ids = Some(Seq(membership.id, membership2.id))).map(_.id) must be(
        Seq(membership.id, membership2.id)
      )

      MembershipsDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
      MembershipsDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
      MembershipsDao.findAll(Authorization.All, ids = Some(Seq(membership.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(membership.id))
    }

    "userId" in {
      MembershipsDao.findAll(Authorization.All, id = Some(membership.id), userId = Some(user.id)).map(_.id) must be(
        Seq(membership.id)
      )

      MembershipsDao.findAll(Authorization.All, id = Some(membership.id), userId = Some(UUID.randomUUID.toString)) must be(Nil)
    }

    "organizationId" in {
      MembershipsDao.findAll(Authorization.All, id = Some(membership.id), organizationId = Some(membership.organization.id)).map(_.id) must be(
        Seq(membership.id)
      )

      MembershipsDao.findAll(Authorization.All, id = Some(membership.id), organizationId = Some(UUID.randomUUID.toString)) must be(Nil)
    }
  }

}
