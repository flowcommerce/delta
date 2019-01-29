package db

import java.util.UUID

import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec

class ShasDaoSpec extends FlowPlaySpec with Helpers {

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
    shasDao.findById(Authorization.All, sha.id) must be(None)
  }

  "findById" in {
    val sha = createSha()
    shasDao.findById(Authorization.All, sha.id).map(_.id) must be(
      Some(sha.id)
    )

    shasDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findByProjectIdAndBranch" in {
    val project = createProject()

    val masterForm = createShaForm(project).copy(branch = "master")
    rightOrErrors(shasWriteDao.create(systemUser, masterForm))

    val fooForm = createShaForm(project).copy(branch = "foo")
    rightOrErrors(shasWriteDao.create(systemUser, fooForm))

    shasDao.findByProjectIdAndBranch(Authorization.All, project.id, "master").map(_.hash) must be(Some(masterForm.hash))
    shasDao.findByProjectIdAndBranch(Authorization.All, project.id, "foo").map(_.hash) must be(Some(fooForm.hash))
    shasDao.findByProjectIdAndBranch(Authorization.All, project.id, "other") must be(None)
  }

  "findAll by ids" in {
    val sha1 = createSha()
    val sha2 = createSha()

    shasDao.findAll(Authorization.All, ids = Some(Seq(sha1.id, sha2.id)), limit = None).map(_.id).sorted must be(
      Seq(sha1.id, sha2.id).sorted
    )

    shasDao.findAll(Authorization.All, ids = Some(Nil), limit = None) must be(Nil)
    shasDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString)), limit = None) must be(Nil)
    shasDao.findAll(Authorization.All, ids = Some(Seq(sha1.id, UUID.randomUUID.toString)), limit = None).map(_.id) must be(Seq(sha1.id))
  }

  "findAll by projectId" in {
    val project1 = createProject()
    val project2 = createProject()

    val sha1 = createSha(createShaForm(project1))
    val sha2 = createSha(createShaForm(project2))

    shasDao.findAll(Authorization.All, projectId = Some(project1.id), limit = None).map(_.id).sorted must be(
      Seq(sha1.id)
    )

    shasDao.findAll(Authorization.All, projectId = Some(project2.id), limit = None).map(_.id).sorted must be(
      Seq(sha2.id)
    )

    shasDao.findAll(Authorization.All, projectId = Some(createTestKey()), limit = None) must be(Nil)
  }

  "validate" must {

    "require sha" in {
      shasWriteDao.validate(createShaForm().copy(hash = "   ")) must be(
        Seq("Hash cannot be empty")
      )
    }

    "require branch" in {
      shasWriteDao.validate(createShaForm().copy(branch = "   ")) must be(
        Seq("Branch cannot be empty")
      )
    }

    "validate project exists" in {
      shasWriteDao.validate(createShaForm().copy(projectId = createTestKey())) must be(
        Seq("Project not found")
      )
    }
 
    "validate existing record" in {
      val form = createShaForm()
      createSha(form)

      shasWriteDao.validate(form) must be(
        Seq("Project already has a hash for this branch")
      )

      shasWriteDao.validate(form.copy(branch = createTestKey())) must be(Nil)
    }

  }

  "authorization for shas" in {
    val org = createOrganization()
    val project = createProject(org)
    val user = createUserReference()
    createMembership(createMembershipForm(org = org, user = user))

    val sha = createSha(createShaForm(project), user = user)

    shasDao.findAll(Authorization.PublicOnly, ids = Some(Seq(sha.id)), limit = None) must be(Nil)
    shasDao.findAll(Authorization.All, ids = Some(Seq(sha.id)), limit = None).map(_.id) must be(Seq(sha.id))
    shasDao.findAll(Authorization.Organization(org.id), ids = Some(Seq(sha.id)), limit = None).map(_.id) must be(Seq(sha.id))
    shasDao.findAll(Authorization.Organization(createOrganization().id), ids = Some(Seq(sha.id)), limit = None) must be(Nil)
    shasDao.findAll(Authorization.User(user.id), ids = Some(Seq(sha.id)), limit = None).map(_.id) must be(Seq(sha.id))
    shasDao.findAll(Authorization.User(createUser().id), ids = Some(Seq(sha.id)), limit = None) must be(Nil)
  }

}
