package db

import java.util.UUID

import io.flow.common.v0.models.{Name, User, UserReference}
import io.flow.delta.api.lib.GitHubHelper
import io.flow.delta.config.v0.{models => config}
import io.flow.delta.lib.config.Defaults
import io.flow.delta.v0.models._
import io.flow.play.util.{Constants, Random}
import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec
import play.api.Application

import scala.concurrent.ExecutionContext

trait Helpers {
  self: FlowPlaySpec =>

  import scala.language.implicitConversions

  def injector = app.injector

  implicit def ec(implicit app: Application): ExecutionContext = app.injector.instanceOf[ExecutionContext]

  lazy val database = init[play.api.db.Database]

  lazy val buildDesiredStatesDao = init[BuildDesiredStatesDao]
  lazy val buildLastStatesDao = init[BuildLastStatesDao]
  lazy val buildsDao = init[BuildsDao]
  lazy val buildsWriteDao = init[BuildsWriteDao]
  lazy val configsDao = init[ConfigsDao]
  lazy val dashboardBuildsDao = init[DashboardBuildsDao]
  lazy val delete = init[Delete]
  lazy val eventsDao = init[EventsDao]
  lazy val githubHelper = init[GitHubHelper]
  lazy val githubUsersDao = init[GithubUsersDao]
  lazy val imagesDao = init[ImagesDao]
  lazy val imagesWriteDao = init[ImagesWriteDao]
  lazy val itemsDao = init[ItemsDao]
  lazy val membershipsDao = init[MembershipsDao]
  lazy val organizationsDao = init[OrganizationsDao]
  lazy val organizationsWriteDao = init[OrganizationsWriteDao]
  lazy val projectsDao = init[ProjectsDao]
  lazy val projectsWriteDao = init[ProjectsWriteDao]
  lazy val shasDao = init[ShasDao]
  lazy val shasWriteDao = init[ShasWriteDao]
  lazy val subscriptionsDao = init[SubscriptionsDao]
  lazy val tagsDao = init[TagsDao]
  lazy val tagsWriteDao = init[TagsWriteDao]
  lazy val tokensDao = init[TokensDao]
  lazy val userIdentifiersDao = init[UserIdentifiersDao]
  lazy val usersDao = init[UsersDao]
  lazy val usersWriteDao = init[UsersWriteDao]
  lazy val variablesDao = init[VariablesDao]

  val random = Random()
  val systemUser = Constants.SystemUser

  def createTestKey(): String = {
    s"z-test-${UUID.randomUUID.toString.toLowerCase}"
  }

  def createTestVersion(): String = {
    s"0.0.${scala.util.Random.nextInt(100)}"
  }

  /**
    * Function called on each iteration until it returns true, up
    * until maxAttempts (at which point an error is raised)
    */
  def waitFor(
    function: () => Boolean,
    maxAttempts: Int = 25,
    msBetweenAttempts: Int = 250
  ): Boolean = {
    var ctr = 0
    var found = false
    while (!found) {
      found = function()
      ctr += 1
      if (ctr > maxAttempts) {
        sys.error("Did not create user organization")
      }
      Thread.sleep(msBetweenAttempts)
    }
    true
  }

  def createOrganization(
    form: OrganizationForm = createOrganizationForm(),
    user: UserReference = Constants.SystemUser
  ): Organization = {
    organizationsWriteDao.create(UserReference(user.id), form).right.getOrElse {
      sys.error("Failed to create organization")
    }
  }

  def createOrganizationForm() = {
    OrganizationForm(
      id = createTestKey(),
      docker = Docker(provider=DockerProvider.DockerHub, organization="flowcommerce"),
      travis = Travis(organization = "flowcommerce")
    )
  }

  def createProject(
    org: Organization = createOrganization()
  ) (
    implicit form: ProjectForm = createProjectForm(org)
  ): Project = {
    val user = organizationsDao.findById(Authorization.All, form.organization).flatMap { org =>
      usersDao.findById(org.user.id)
    }.getOrElse {
      sys.error("Could not find user that created org")
    }

    rightOrErrors(projectsWriteDao.create(UserReference(user.id), form))
  }

  def createProjectForm(
    org: Organization = createOrganization()
  ) = {
    ProjectForm(
      organization = org.id,
      name = createTestName(),
      visibility = Visibility.Private,
      scms = Scms.Github,
      uri = s"http://github.com/test/${UUID.randomUUID.toString}",
      config = None
    )
  }

  def makeUser(
    form: UserForm = makeUserForm()
  ): User = {
    User(
      id = io.flow.play.util.IdGenerator("tst").randomId(),
      email = form.email,
      name = form.name.getOrElse(Name())
    )
  }

  def makeUserForm() = UserForm(
    email = None,
    name = None
  )

  def createUser(
    form: UserForm = createUserForm()
  ): User = {
    rightOrErrors(usersWriteDao.create(None, form))
  }

  def createUserReference(
    form: UserForm = createUserForm()
  ): UserReference = {
    UserReference(rightOrErrors(usersWriteDao.create(None, form)).id)
  }

  def createUserForm(
    email: String = createTestEmail(),
    name: Option[Name] = None
  ) = UserForm(
    email = Some(email),
    name = name
  )

  def createGithubUser(
    form: GithubUserForm = createGithubUserForm()
  ): GithubUser = {
    githubUsersDao.create(None, form)
  }

  def createGithubUserForm(
    user: UserReference = createUserReference(),
    githubUserId: Long = random.positiveLong(),
    login: String = createTestKey()
  ) = {
    GithubUserForm(
      userId = user.id,
      githubUserId = githubUserId,
      login = login
    )
  }

  def createToken(
    form: TokenForm = createTokenForm()
  ): Token = {
    rightOrErrors(tokensDao.create(Constants.SystemUser, InternalTokenForm.UserCreated(form)))
  }

  def createTokenForm(
    user: UserReference = createUserReference()
  ):TokenForm = {
    TokenForm(
      userId = user.id,
      description = None
    )
  }

  def createMembership(
    form: MembershipForm = createMembershipForm()
  ): Membership = {
    rightOrErrors(membershipsDao.create(Constants.SystemUser, form))
  }

  def createMembershipForm(
    org: Organization = createOrganization(),
    user: UserReference = createUserReference(),
    role: Role = Role.Member
  ) = {
    MembershipForm(
      organization = org.id,
      userId = user.id,
      role = role
    )
  }

  def replaceItem(
    org: Organization
  ) (
    implicit form: ItemForm = createItemForm(org)
  ): Item = {
    itemsDao.replace(Constants.SystemUser, form)
  }

  def createItemSummary(
    org: Organization
  ) (
    implicit project: Project = createProject(org)
  ): ItemSummary = {
    ProjectSummary(
      id = project.id,
      organization = OrganizationSummary(org.id),
      name = project.name,
      uri = project.uri
    )
  }

  def createItemForm(
    org: Organization
  ) (
    implicit summary: ItemSummary = createItemSummary(org)
  ): ItemForm = {
    val label = summary match {
      case ProjectSummary(id, org, name, uri) => name
      case ItemSummaryUndefinedType(name) => name
    }
    ItemForm(
      summary = summary,
      label = label,
      description = None,
      contents = label
    )
  }

  def createSubscription(
    form: SubscriptionForm = createSubscriptionForm()
  ): Subscription = {
    /**
     * SubscriptionsDao.create gets into a race condition
     * with UserWritesDao.create, which is called when a user
     * is created in createSubscriptionForm above. When a user
     * is created, it will trigger auto subscription in UserActor.Messages.Created
     **/
    subscriptionsDao.upsert(Constants.SystemUser, form)
  }

  def createSubscriptionForm(
    user: UserReference = createUserReference(),
    publication: Publication = Publication.Deployments
  ) = {
    SubscriptionForm(
      userId = user.id,
      publication = publication
    )
  }

  def upsertBuild(
    project: Project = createProject()
  ) (
    implicit cfg: config.Build = createBuildConfig(project),
             user: UserReference = Constants.SystemUser
  ): Build = {
    buildsWriteDao.upsert(UserReference(user.id), project.id, Status.Enabled, cfg)
  }

  def createBuildConfig(
    project: Project = createProject()
  ) = {
    Defaults.Build.copy(
      name = createTestKey()
    )
  }

  def createSha(
    form: ShaForm = createShaForm(),
    user: UserReference = Constants.SystemUser
  ): Sha = {
    rightOrErrors(injector.instanceOf[ShasWriteDao].create(UserReference(user.id), form))
  }

  def createShaForm(
    project: Project = createProject()
  ) = {
    ShaForm(
      projectId = project.id,
      branch = createTestKey(),
      hash = createTestKey()
    )
  }

  def createTag(
    form: TagForm = createTagForm(),
    user: UserReference = Constants.SystemUser
  ): Tag = {
    rightOrErrors(tagsWriteDao.create(UserReference(user.id), form))
  }

  def createTagForm(
    project: Project = createProject()
  ) = {
    TagForm(
      projectId = project.id,
      name = "0.0.1",
      hash = UUID.randomUUID.toString().replaceAll("-", "")
    )
  }

  def createEvent(
    project: Project = createProject(),
    action: EventType = EventType.Info,
    summary: String = "test",
    ex: Option[Throwable] = None
  ): Event = {
    val id = eventsDao.create(Constants.SystemUser, project.id, action, summary, ex)
    eventsDao.findById(id).getOrElse {
      sys.error("Failed to create event")
    }
  }

  def createImage(
   form: ImageForm = createImageForm(),
   user: UserReference = Constants.SystemUser
  ): Image = {
    rightOrErrors(imagesWriteDao.create(UserReference(user.id), form))
  }

  def createImageForm(
   build: Build = upsertBuild()
  ) = {
    ImageForm(
      buildId = build.id,
      name = createTestKey(),
      version = createTestVersion()
    )
  }

  def setLastState(build: Build, tag: String, instances: Long) {
    setLastStates(
      build,
      Seq(
        Version(
          name = tag,
          instances = instances
        )
      )
    )
  }

  def setLastStates(build: Build, versions: Seq[Version]) {
    rightOrErrors(
      buildLastStatesDao.upsert(Constants.SystemUser, build, StateForm(versions = versions))
    )
  }

}
