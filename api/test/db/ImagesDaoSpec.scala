package db

import java.util.UUID

import io.flow.test.utils.FlowPlaySpec

class ImagesDaoSpec extends FlowPlaySpec with Helpers {

  "create" in {
    val build = upsertBuild()
    val form = createImageForm(build)
    val image = rightOrErrors(imagesWriteDao.create(systemUser, form))
    image.build.id must be(build.id)
    image.name must be(form.name)
    image.version must be(form.version)

    imagesWriteDao.delete(systemUser, image)
    imagesDao.findById(image.id) must be(None)
  }

  "upsert" in {
    val build = upsertBuild()
    val name = "test"
    val version = "0.1.0"

    val image = imagesWriteDao.upsert(systemUser, build.id, name, version)
    image.name must be(name)
    image.version must be(version)

    val image2 = imagesWriteDao.upsert(systemUser, build.id, name, version)
    image2.id must be(image.id)
    image2.name must be(name)
    image2.version must be(version)

    val otherName = "test2"
    val image3 = imagesWriteDao.upsert(systemUser, build.id, otherName, version)
    image3.id must be(image2.id)
    image3.name must be(otherName)
    image3.version must be(version)

    imagesWriteDao.delete(systemUser, image)
    imagesWriteDao.delete(systemUser, image2)
    imagesWriteDao.delete(systemUser, image3)
  }

  "delete" in {
    val image = createImage()
    imagesWriteDao.delete(systemUser, image)
    imagesDao.findById(image.id) must be(None)
  }

  "findById" in {
    val image = createImage()
    imagesDao.findById(image.id).map(_.id) must be(
      Some(image.id)
    )

    imagesDao.findById(UUID.randomUUID.toString) must be(None)

    imagesWriteDao.delete(systemUser, image)
  }

  "findByBuildIdAndVersion" in {
    val build = upsertBuild()

    val form1 = createImageForm(build).copy(version = "0.0.2")
    val tag1 = rightOrErrors(imagesWriteDao.create(systemUser, form1))

    val form2 = createImageForm(build).copy(version = "0.0.3")
    val tag2 = rightOrErrors(imagesWriteDao.create(systemUser, form2))

    val image = imagesDao.findByBuildIdAndVersion(build.id, "0.0.2")
    val image2 = imagesDao.findByBuildIdAndVersion(build.id, "0.0.3")

    image.map(_.id) must be(Some(tag1.id))
    image2.map(_.id) must be(Some(tag2.id))
    imagesDao.findByBuildIdAndVersion(build.id, "other") must be(None)

    imagesWriteDao.delete(systemUser, image.get)
    imagesWriteDao.delete(systemUser, image2.get)
  }

  "findAll by ids" in {
    val image1 = createImage()
    val image2 = createImage()

    imagesDao.findAll(ids = Some(Seq(image1.id, image2.id))).map(_.id).sorted must be(
      Seq(image1.id, image2.id).sorted
    )

    imagesDao.findAll(ids = Some(Nil)) must be(Nil)
    imagesDao.findAll(ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    imagesDao.findAll(ids = Some(Seq(image1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(image1.id))

    imagesWriteDao.delete(systemUser, image1)
    imagesWriteDao.delete(systemUser, image2)
  }

  "findAll by buildId" in {
    val build1 = upsertBuild()
    val build2 = upsertBuild()

    val image1 = createImage(createImageForm(build1))
    val image2 = createImage(createImageForm(build2))

    imagesDao.findAll(buildId = Some(build1.id)).map(_.id).sorted must be(
      Seq(image1.id)
    )

    imagesDao.findAll(buildId = Some(build2.id)).map(_.id).sorted must be(
      Seq(image2.id)
    )

    imagesDao.findAll(buildId = Some(createTestKey())) must be(Nil)

    imagesWriteDao.delete(systemUser, image1)
    imagesWriteDao.delete(systemUser, image2)
  }

  "validate" must {
    "require version" in {
      imagesWriteDao.validate(systemUser, createImageForm().copy(version = "   ")) must be(
        Seq("Version cannot be empty")
      )
    }

    "require name" in {
      imagesWriteDao.validate(systemUser, createImageForm().copy(name = "   ")) must be(
        Seq("Name cannot be empty")
      )
    }

    "validate version is semver" in {
      imagesWriteDao.validate(systemUser, createImageForm().copy(version = "release")) must be(
        Seq("Version must match semver pattern (e.g. 0.1.2)")
      )
    }

    "validate build exists" in {
      imagesWriteDao.validate(systemUser, createImageForm().copy(buildId = createTestKey())) must be(
        Seq("Build not found")
      )
    }
  }
}
