package actors.functions

import io.flow.delta.actors.SupervisorResult
import io.flow.delta.config.v0.models.InstanceType
import io.flow.delta.config.v0.{models => config}
import io.flow.delta.v0.models._
import io.flow.test.utils.FlowPlaySpec

//These tests require a valid AWS account & secret key
class SyncECRImagesSpec extends FlowPlaySpec with db.Helpers {
  private val syncECR =  app.injector.instanceOf[SyncECRImages]

  val bConfig = config.Build(
    createTestId(),
    createTestId(),
    1,
    InstanceType.M52xlarge,
    portContainer = 1,
    portHost = 1,
    stages = Nil,
    dependencies = Nil,
    version = Some("1.0.0")
  )

  "sync non-existant repo" ignore {
    syncECR.run(
      Build(
        createTestId(),
        ProjectSummary(
          createTestId(),
          OrganizationSummary("flowcommerce"),
          createTestId(),
          createTestId()
        ),
        Status.Enabled,
        "root"),
      bConfig
    ) match {
      case SupervisorResult.Error(_, _) =>
      case _ => fail("Should have failed but didn't")
    }
  }

  "sync existing repo" ignore {
    projectsWriteDao.create(systemUser, ProjectForm("flowcommerce", "registry", Visibility.Private, Scms.Github, "https://github.com/flowcommerce/registry", None))
    val b = buildsWriteDao.upsert(systemUser, "registry", Status.Enabled, bConfig)

    syncECR.run(
      Build(
        b.id,
        ProjectSummary(
          "registry",
          OrganizationSummary("flowcommerce"),
          createTestId(),
          createTestId()
        ),
        Status.Enabled,
        "root"),
      bConfig
    ) match {
      case SupervisorResult.Change(_) =>
      case _ => fail("Should have changed but didn't")
    }

    imagesDao.findAll(buildId = Some(b.id)).nonEmpty must be(true)
  }
}
