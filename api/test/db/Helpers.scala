package db

import io.flow.postgresql.Authorization
import io.flow.play.util.Random
import io.flow.delta.config.v0.{models => config}
import io.flow.delta.lib.config.Defaults
import io.flow.delta.v0.models._
import io.flow.common.v0.models.{Name, User, UserReference}
import java.util.UUID

trait Helpers {

  import scala.language.implicitConversions
  implicit def toUserReference(user: User) = UserReference(id = user.id)

  lazy val systemUser = createUser()

  lazy val buildDesiredStatesWriteDao = play.api.Play.current.injector.instanceOf[BuildDesiredStatesWriteDao]
  lazy val buildLastStatesWriteDao = play.api.Play.current.injector.instanceOf[BuildLastStatesWriteDao]
  
  def injector = play.api.Play.current.injector

  val random = Random()

  def createTestEmail(): String = {
    s"$createTestKey@test.bryzek.com"
  }

  def createTestName(): String = {
    s"Z Test ${UUID.randomUUID.toString}"
  }

  def createTestKey(): String = {
    s"z-test-${UUID.randomUUID.toString.toLowerCase}"
  }

  def createTestVersion(): String = {
    s"0.0.${scala.util.Random.nextInt(100)}"
  }

  def rightOrErrors[T](result: Either[Seq[String], T]): T = {
    result match {
      case Left(errors) => sys.error(errors.mkString(", "))
      case Right(obj) => obj
    }
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
    user: User = systemUser
  ): Organization = {
    injector.instanceOf[OrganizationsWriteDao].create(user, form).right.getOrElse {
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
    val user = OrganizationsDao.findById(Authorization.All, form.organization).flatMap { org =>
      UsersDao.findById(org.user.id)
    }.getOrElse {
      sys.error("Could not find user that created org")
    }

    rightOrErrors(injector.instanceOf[ProjectsWriteDao].create(user, form))
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
    rightOrErrors(injector.instanceOf[UsersWriteDao].create(None, form))
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
    GithubUsersDao.create(None, form)
  }

  def createGithubUserForm(
    user: User = createUser(),
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
    rightOrErrors(TokensDao.create(systemUser, InternalTokenForm.UserCreated(form)))
  }

  def createTokenForm(
    user: User = createUser()
  ):TokenForm = {
    TokenForm(
      userId = user.id,
      description = None
    )
  }

  def createMembership(
    form: MembershipForm = createMembershipForm()
  ): Membership = {
    rightOrErrors(MembershipsDao.create(systemUser, form))
  }

  def createMembershipForm(
    org: Organization = createOrganization(),
    user: User = createUser(),
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
    ItemsDao.replace(systemUser, form)
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
    //rightOrErrors(SubscriptionsDao.create(systemUser, form))
    SubscriptionsDao.upsert(systemUser, form)
  }

  def createSubscriptionForm(
    user: User = createUser(),
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
             user: User = systemUser
  ): Build = {
    injector.instanceOf[BuildsWriteDao].upsert(user, project.id, Status.Enabled, cfg)
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
    user: User = systemUser
  ): Sha = {
    rightOrErrors(injector.instanceOf[ShasWriteDao].create(user, form))
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
    user: User = systemUser
  ): Tag = {
    rightOrErrors(injector.instanceOf[TagsWriteDao].create(user, form))
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
    val id = EventsDao.create(systemUser, project.id, action, summary, ex)
    EventsDao.findById(id).getOrElse {
      sys.error("Failed to create event")
    }
  }

  def createImage(
   form: ImageForm = createImageForm(),
   user: User = systemUser
  ): Image = {
    rightOrErrors(injector.instanceOf[ImagesWriteDao].create(user, form))
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
      buildLastStatesWriteDao.upsert(systemUser, build, StateForm(versions = versions))
    )
  }

}
