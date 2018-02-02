package db

import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class ShasDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val shasWriteDao = app.injector.instanceOf[ShasWriteDao]

  "create" in {
    val project = createProject()
    val hash = createTestKey()
    val form = createShaForm(project).copy(branch = "master", hash = hash)
    val sha = rightOrErrors(shasWriteDao.create(systemUser, form))
    sha.project.id must be(project.id)
    sha.branch must be("master")
    sha.hash must be(hash)
  }

  "upsertBranch" in {
    val project = createProject()

    val hash = createTestKey()

    val sha = shasWriteDao.upsertBranch(systemUser, project.id, "master", hash)
    sha.hash must be(hash)

    val sha2 = shasWriteDao.upsertBranch(systemUser, project.id, "master", hash)
    sha2.hash must be(hash)

    val other = createTestKey()
    val sha3 = shasWriteDao.upsertBranch(systemUser, project.id, "master", other)
    sha3.hash must be(other)
  }

  "delete" in {
    val sha = createSha()
    shasWriteDao.delete(systemUser, sha)
    ShasDao.findById(Authorization.All, sha.id) must be(None)
  }

  "findById" in {
    val sha = createSha()
    ShasDao.findById(Authorization.All, sha.id).map(_.id) must be(
      Some(sha.id)
    )

    ShasDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findByProjectIdAndBranch" in {
    val project = createProject()

    val masterForm = createShaForm(project).copy(branch = "master")
    val master = rightOrErrors(shasWriteDao.create(systemUser, masterForm))

    val fooForm = createShaForm(project).copy(branch = "foo")
    val foo = rightOrErrors(shasWriteDao.create(systemUser, fooForm))

    ShasDao.findByProjectIdAndBranch(Authorization.All, project.id, "master").map(_.hash) must be(Some(masterForm.hash))
    ShasDao.findByProjectIdAndBranch(Authorization.All, project.id, "foo").map(_.hash) must be(Some(fooForm.hash))
    ShasDao.findByProjectIdAndBranch(Authorization.All, project.id, "other") must be(None)
  }

  "findAll by ids" in {
    val sha1 = createSha()
    val sha2 = createSha()

    ShasDao.findAll(Authorization.All, ids = Some(Seq(sha1.id, sha2.id))).map(_.id).sorted must be(
      Seq(sha1.id, sha2.id).sorted
    )

    ShasDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
    ShasDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    ShasDao.findAll(Authorization.All, ids = Some(Seq(sha1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(sha1.id))
  }

  "findAll by projectId" in {
    val project1 = createProject()
    val project2 = createProject()

    val sha1 = createSha(createShaForm(project1))
    val sha2 = createSha(createShaForm(project2))

    ShasDao.findAll(Authorization.All, projectId = Some(project1.id)).map(_.id).sorted must be(
      Seq(sha1.id)
    )

    ShasDao.findAll(Authorization.All, projectId = Some(project2.id)).map(_.id).sorted must be(
      Seq(sha2.id)
    )

    ShasDao.findAll(Authorization.All, projectId = Some(createTestKey())) must be(Nil)
  }

  "validate" must {

    "require sha" in {
      shasWriteDao.validate(systemUser, createShaForm().copy(hash = "   ")) must be(
        Seq("Hash cannot be empty")
      )
    }

    "require branch" in {
      shasWriteDao.validate(systemUser, createShaForm().copy(branch = "   ")) must be(
        Seq("Branch cannot be empty")
      )
    }

    "validate project exists" in {
      shasWriteDao.validate(systemUser, createShaForm().copy(projectId = createTestKey())) must be(
        Seq("Project not found")
      )
    }
 
    "validate existing record" in {
      val form = createShaForm()
      val sha = createSha(form)

      shasWriteDao.validate(systemUser, form) must be(
        Seq("Project already has a hash for this branch")
      )

      shasWriteDao.validate(systemUser, form.copy(branch = createTestKey())) must be(Nil)
    }

  }

  "authorization for shas" in {
    val org = createOrganization()
    val project = createProject(org)
    val user = createUser()
    createMembership(createMembershipForm(org = org, user = user))

    val sha = createSha(createShaForm(project), user = user)

    ShasDao.findAll(Authorization.PublicOnly, ids = Some(Seq(sha.id))) must be(Nil)
    ShasDao.findAll(Authorization.All, ids = Some(Seq(sha.id))).map(_.id) must be(Seq(sha.id))
    ShasDao.findAll(Authorization.Organization(org.id), ids = Some(Seq(sha.id))).map(_.id) must be(Seq(sha.id))
    ShasDao.findAll(Authorization.Organization(createOrganization().id), ids = Some(Seq(sha.id))) must be(Nil)
    ShasDao.findAll(Authorization.User(user.id), ids = Some(Seq(sha.id))).map(_.id) must be(Seq(sha.id))
    ShasDao.findAll(Authorization.User(createUser().id), ids = Some(Seq(sha.id))) must be(Nil)
  }

}
