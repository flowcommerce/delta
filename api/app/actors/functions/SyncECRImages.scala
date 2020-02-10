package actors.functions

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.ecr.model.DescribeImagesRequest
import db.{ImagesDao, ImagesWriteDao, OrganizationsDao}
import io.flow.delta.actors.SupervisorResult
import io.flow.delta.aws.{Configuration, Credentials}
import io.flow.delta.config.v0.models.BuildConfig
import io.flow.delta.lib.{BuildNames, Semver}
import io.flow.delta.v0.models.{Build, Docker, ImageForm}
import io.flow.postgresql.Authorization
import io.flow.util.Constants
import javax.inject.Inject

import scala.jdk.CollectionConverters._


/**
  * Downloads all tags from docker hub and stores in local DB
  */
class SyncECRImages @Inject()(
  imagesDao: ImagesDao,
  imagesWriteDao: ImagesWriteDao,
  organizationsDao: OrganizationsDao,
  credentials: Credentials,
  configuration: Configuration
) {

  private[this] lazy val ecrclient = com.amazonaws.services.ecr.AmazonECRClient
    .builder().
    withCredentials(new AWSStaticCredentialsProvider(credentials.aws)).
    withClientConfiguration(configuration.aws).
    build()

  def run(build: Build, cfg: BuildConfig): SupervisorResult = {
    organizationsDao.findById(Authorization.All, build.project.organization.id) match {
      case None => {
        // build was deleted
        SupervisorResult.Ready(s"Build org[${build.project.organization.id}] not found - nothing to do")
      }

      case Some(org) => {
        syncImages(org.docker, build, cfg)
      }
    }
  }

  def syncImages(
    docker: Docker,
    build: Build,
    cfg: BuildConfig,
  ): SupervisorResult = {
    try {
      val descriedImages = ecrclient
        .describeImages(new DescribeImagesRequest().withRepositoryName(BuildNames.projectName(build)))
        .getImageDetails
        .asScala

      descriedImages.flatMap { di =>
        val versionTags = di.getImageTags.asScala.filter(Semver.isSemver)
        versionTags.filter { vt =>
          upsertImage(docker, build, cfg, vt)
        }
      }.toList match {
        case Nil => SupervisorResult.Ready("No new ECR images found")
        case tag :: Nil => SupervisorResult.Change(s"ECR image[$tag] synced")
        case multiple => SupervisorResult.Change(s"ECR images[${multiple.mkString(", ")}] synced")
      }
    } catch {
      case ex: Throwable => {
        SupervisorResult.Error(s"${BuildNames.projectName(build)} Error fetching ECR tags for build id[${build.id}]", Some(ex))
      }
    }
  }

  private[this] def upsertImage(docker: Docker, build: Build, cfg: BuildConfig, version: String): Boolean = {
    imagesDao.findByBuildIdAndVersion(build.id, version) match {
      case Some(_) => {
        // Already know about this tag
        false
      }

      case None => {
        imagesWriteDao.create(
          Constants.SystemUser,
          ImageForm(
            buildId = build.id,
            name = BuildNames.dockerImageName(docker, build, cfg),
            version = version
          )
        ) match {
          case Left(msgs) => sys.error(msgs.mkString(", "))
          case Right(_) => true
        }
      }
    }
  }

}
