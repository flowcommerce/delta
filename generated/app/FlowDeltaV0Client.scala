/**
 * Generated by apidoc - http://www.apidoc.me
 * Service version: 0.0.1
 * apidoc:0.11.7 http://www.apidoc.me/flow/delta/0.0.1/play_2_4_client
 */
package io.flow.delta.v0.models {

  /**
   * Represents a deployment of an image to production
   */
  case class Deployment(
    id: String,
    image: io.flow.delta.v0.models.Image,
    user: io.flow.common.v0.models.User,
    events: Seq[io.flow.delta.v0.models.Event]
  )

  /**
   * Discrete individual events related to a deployment.
   */
  case class Event(
    id: String,
    `type`: io.flow.delta.v0.models.EventType,
    timestamp: _root_.org.joda.time.DateTime,
    message: _root_.scala.Option[String] = None
  )

  /**
   * An image represents a tagged Docker Hub image.
   */
  case class Image(
    id: String
  )

  /**
   * A project represents a Dockerized service deployable via the Delta application.
   */
  case class Project(
    id: String,
    organization: io.flow.common.v0.models.OrganizationSummary,
    masterSha: String
  )

  case class ProjectForm(
    id: String,
    organization: String,
    masterSha: String
  )

  case class ProjectPutForm(
    organization: String,
    masterSha: String
  )

  case class ProjectReference(
    id: String
  )

  case class ProjectVersion(
    id: String,
    timestamp: _root_.org.joda.time.DateTime,
    `type`: io.flow.common.v0.models.ChangeType,
    project: io.flow.delta.v0.models.Project
  )

  /**
   * Github repository tag.
   */
  case class Tag(
    id: String
  )

  /**
   * Types of events that could happen during the deployment process
   */
  sealed trait EventType

  object EventType {

    /**
     * Deployment in queued, but has not started.
     */
    case object Queue extends EventType { override def toString = "queue" }
    /**
     * Deployment has started and waiting for updates
     */
    case object Start extends EventType { override def toString = "start" }
    /**
     * Creation new AWS ECS task definition
     */
    case object EcsTaskCreate extends EventType { override def toString = "ecs_task_create" }
    /**
     * Creation of new AWS ECS service
     */
    case object EcsTask extends EventType { override def toString = "ecs_task" }
    /**
     * ECS Task and Service have been set up
     */
    case object Deploy extends EventType { override def toString = "deploy" }
    /**
     * Transferring traffic load from old task to new task
     */
    case object TrafficBalance extends EventType { override def toString = "traffic_balance" }
    /**
     * All done and everything looks good!
     */
    case object Complete extends EventType { override def toString = "complete" }
    /**
     * Some issue occurred...
     */
    case object Error extends EventType { override def toString = "error" }
    /**
     * If a rollback was automatically initiated, this records that event
     */
    case object RollbackStart extends EventType { override def toString = "rollback_start" }
    /**
     * Rollback has been completed. Back to the old normal.
     */
    case object RollbackComplete extends EventType { override def toString = "rollback_complete" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends EventType

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Queue, Start, EcsTaskCreate, EcsTask, Deploy, TrafficBalance, Complete, Error, RollbackStart, RollbackComplete)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): EventType = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[EventType] = byName.get(value.toLowerCase)

  }

}

package io.flow.delta.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.flow.common.v0.models.json._
    import io.flow.delta.v0.models.json._

    private[v0] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[v0] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[v0] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[v0] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val jsonReadsDeltaEventType = new play.api.libs.json.Reads[io.flow.delta.v0.models.EventType] {
      def reads(js: play.api.libs.json.JsValue): play.api.libs.json.JsResult[io.flow.delta.v0.models.EventType] = {
        js match {
          case v: play.api.libs.json.JsString => play.api.libs.json.JsSuccess(io.flow.delta.v0.models.EventType(v.value))
          case _ => {
            (js \ "value").validate[String] match {
              case play.api.libs.json.JsSuccess(v, _) => play.api.libs.json.JsSuccess(io.flow.delta.v0.models.EventType(v))
              case err: play.api.libs.json.JsError => err
            }
          }
        }
      }
    }

    def jsonWritesDeltaEventType(obj: io.flow.delta.v0.models.EventType) = {
      play.api.libs.json.JsString(obj.toString)
    }

    def jsObjectEventType(obj: io.flow.delta.v0.models.EventType) = {
      play.api.libs.json.Json.obj("value" -> play.api.libs.json.JsString(obj.toString))
    }

    implicit def jsonWritesDeltaEventType: play.api.libs.json.Writes[EventType] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.EventType] {
        def writes(obj: io.flow.delta.v0.models.EventType) = {
          jsonWritesDeltaEventType(obj)
        }
      }
    }

    implicit def jsonReadsDeltaDeployment: play.api.libs.json.Reads[Deployment] = {
      (
        (__ \ "id").read[String] and
        (__ \ "image").read[io.flow.delta.v0.models.Image] and
        (__ \ "user").read[io.flow.common.v0.models.User] and
        (__ \ "events").read[Seq[io.flow.delta.v0.models.Event]]
      )(Deployment.apply _)
    }

    def jsObjectDeployment(obj: io.flow.delta.v0.models.Deployment) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "image" -> jsObjectImage(obj.image),
        "user" -> io.flow.common.v0.models.json.jsObjectUser(obj.user),
        "events" -> play.api.libs.json.Json.toJson(obj.events)
      )
    }

    implicit def jsonWritesDeltaDeployment: play.api.libs.json.Writes[Deployment] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.Deployment] {
        def writes(obj: io.flow.delta.v0.models.Deployment) = {
          jsObjectDeployment(obj)
        }
      }
    }

    implicit def jsonReadsDeltaEvent: play.api.libs.json.Reads[Event] = {
      (
        (__ \ "id").read[String] and
        (__ \ "type").read[io.flow.delta.v0.models.EventType] and
        (__ \ "timestamp").read[_root_.org.joda.time.DateTime] and
        (__ \ "message").readNullable[String]
      )(Event.apply _)
    }

    def jsObjectEvent(obj: io.flow.delta.v0.models.Event) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "type" -> play.api.libs.json.JsString(obj.`type`.toString),
        "timestamp" -> play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(obj.timestamp))
      ) ++ (obj.message match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("message" -> play.api.libs.json.JsString(x))
      })
    }

    implicit def jsonWritesDeltaEvent: play.api.libs.json.Writes[Event] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.Event] {
        def writes(obj: io.flow.delta.v0.models.Event) = {
          jsObjectEvent(obj)
        }
      }
    }

    implicit def jsonReadsDeltaImage: play.api.libs.json.Reads[Image] = {
      (__ \ "id").read[String].map { x => new Image(id = x) }
    }

    def jsObjectImage(obj: io.flow.delta.v0.models.Image) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id)
      )
    }

    implicit def jsonWritesDeltaImage: play.api.libs.json.Writes[Image] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.Image] {
        def writes(obj: io.flow.delta.v0.models.Image) = {
          jsObjectImage(obj)
        }
      }
    }

    implicit def jsonReadsDeltaProject: play.api.libs.json.Reads[Project] = {
      (
        (__ \ "id").read[String] and
        (__ \ "organization").read[io.flow.common.v0.models.OrganizationSummary] and
        (__ \ "master_sha").read[String]
      )(Project.apply _)
    }

    def jsObjectProject(obj: io.flow.delta.v0.models.Project) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "organization" -> io.flow.common.v0.models.json.jsObjectOrganizationSummary(obj.organization),
        "master_sha" -> play.api.libs.json.JsString(obj.masterSha)
      )
    }

    implicit def jsonWritesDeltaProject: play.api.libs.json.Writes[Project] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.Project] {
        def writes(obj: io.flow.delta.v0.models.Project) = {
          jsObjectProject(obj)
        }
      }
    }

    implicit def jsonReadsDeltaProjectForm: play.api.libs.json.Reads[ProjectForm] = {
      (
        (__ \ "id").read[String] and
        (__ \ "organization").read[String] and
        (__ \ "master_sha").read[String]
      )(ProjectForm.apply _)
    }

    def jsObjectProjectForm(obj: io.flow.delta.v0.models.ProjectForm) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "organization" -> play.api.libs.json.JsString(obj.organization),
        "master_sha" -> play.api.libs.json.JsString(obj.masterSha)
      )
    }

    implicit def jsonWritesDeltaProjectForm: play.api.libs.json.Writes[ProjectForm] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.ProjectForm] {
        def writes(obj: io.flow.delta.v0.models.ProjectForm) = {
          jsObjectProjectForm(obj)
        }
      }
    }

    implicit def jsonReadsDeltaProjectPutForm: play.api.libs.json.Reads[ProjectPutForm] = {
      (
        (__ \ "organization").read[String] and
        (__ \ "master_sha").read[String]
      )(ProjectPutForm.apply _)
    }

    def jsObjectProjectPutForm(obj: io.flow.delta.v0.models.ProjectPutForm) = {
      play.api.libs.json.Json.obj(
        "organization" -> play.api.libs.json.JsString(obj.organization),
        "master_sha" -> play.api.libs.json.JsString(obj.masterSha)
      )
    }

    implicit def jsonWritesDeltaProjectPutForm: play.api.libs.json.Writes[ProjectPutForm] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.ProjectPutForm] {
        def writes(obj: io.flow.delta.v0.models.ProjectPutForm) = {
          jsObjectProjectPutForm(obj)
        }
      }
    }

    implicit def jsonReadsDeltaProjectReference: play.api.libs.json.Reads[ProjectReference] = {
      (__ \ "id").read[String].map { x => new ProjectReference(id = x) }
    }

    def jsObjectProjectReference(obj: io.flow.delta.v0.models.ProjectReference) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id)
      )
    }

    implicit def jsonWritesDeltaProjectReference: play.api.libs.json.Writes[ProjectReference] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.ProjectReference] {
        def writes(obj: io.flow.delta.v0.models.ProjectReference) = {
          jsObjectProjectReference(obj)
        }
      }
    }

    implicit def jsonReadsDeltaProjectVersion: play.api.libs.json.Reads[ProjectVersion] = {
      (
        (__ \ "id").read[String] and
        (__ \ "timestamp").read[_root_.org.joda.time.DateTime] and
        (__ \ "type").read[io.flow.common.v0.models.ChangeType] and
        (__ \ "project").read[io.flow.delta.v0.models.Project]
      )(ProjectVersion.apply _)
    }

    def jsObjectProjectVersion(obj: io.flow.delta.v0.models.ProjectVersion) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "timestamp" -> play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(obj.timestamp)),
        "type" -> play.api.libs.json.JsString(obj.`type`.toString),
        "project" -> jsObjectProject(obj.project)
      )
    }

    implicit def jsonWritesDeltaProjectVersion: play.api.libs.json.Writes[ProjectVersion] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.ProjectVersion] {
        def writes(obj: io.flow.delta.v0.models.ProjectVersion) = {
          jsObjectProjectVersion(obj)
        }
      }
    }

    implicit def jsonReadsDeltaTag: play.api.libs.json.Reads[Tag] = {
      (__ \ "id").read[String].map { x => new Tag(id = x) }
    }

    def jsObjectTag(obj: io.flow.delta.v0.models.Tag) = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id)
      )
    }

    implicit def jsonWritesDeltaTag: play.api.libs.json.Writes[Tag] = {
      new play.api.libs.json.Writes[io.flow.delta.v0.models.Tag] {
        def writes(obj: io.flow.delta.v0.models.Tag) = {
          jsObjectTag(obj)
        }
      }
    }
  }
}

package io.flow.delta.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import io.flow.delta.v0.models._

    // Type: date-time-iso8601
    implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    implicit val queryStringBindableTypeDateTimeIso8601 = new QueryStringBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    // Type: date-iso8601
    implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )

    implicit val queryStringBindableTypeDateIso8601 = new QueryStringBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )

    // Enum: EventType
    private[this] val enumEventTypeNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${io.flow.delta.v0.models.EventType.all.mkString(", ")}"

    implicit val pathBindableEnumEventType = new PathBindable.Parsing[io.flow.delta.v0.models.EventType] (
      EventType.fromString(_).get, _.toString, enumEventTypeNotFound
    )

    implicit val queryStringBindableEnumEventType = new QueryStringBindable.Parsing[io.flow.delta.v0.models.EventType](
      EventType.fromString(_).get, _.toString, enumEventTypeNotFound
    )

  }

}


package io.flow.delta.v0 {

  object Constants {

    val Namespace = "io.flow.delta.v0"
    val UserAgent = "apidoc:0.11.7 http://www.apidoc.me/flow/delta/0.0.1/play_2_4_client"
    val Version = "0.0.1"
    val VersionMajor = 0

  }

  class Client(
    apiUrl: String,
    auth: scala.Option[io.flow.delta.v0.Authorization] = None,
    defaultHeaders: Seq[(String, String)] = Nil
  ) extends interfaces.Client {
    import io.flow.common.v0.models.json._
    import io.flow.delta.v0.models.json._

    private[this] val logger = play.api.Logger("io.flow.delta.v0.Client")

    logger.info(s"Initializing io.flow.delta.v0.Client for url $apiUrl")

    def deployments: Deployments = Deployments

    def healthchecks: Healthchecks = Healthchecks

    def images: Images = Images

    def projects: Projects = Projects

    def tags: Tags = Tags

    object Deployments extends Deployments {
      override def get(
        projectId: String,
        id: _root_.scala.Option[String] = None,
        ids: _root_.scala.Option[Seq[String]] = None,
        limit: Long = 25,
        offset: Long = 0
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Deployment]] = {
        val queryParameters = Seq(
          id.map("id" -> _),
          Some("limit" -> limit.toString),
          Some("offset" -> offset.toString)
        ).flatten ++
          ids.getOrElse(Nil).map("ids" -> _)

        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(projectId, "UTF-8")}/deployments", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("Seq[io.flow.delta.v0.models.Deployment]", r, _.validate[Seq[io.flow.delta.v0.models.Deployment]])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r if r.status == 422 => throw new io.flow.delta.v0.errors.ErrorsResponse(r)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 422")
        }
      }

      override def getById(
        projectId: String,
        id: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.delta.v0.models.Deployment] = {
        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(projectId, "UTF-8")}/deployments/${play.utils.UriEncoding.encodePathSegment(id, "UTF-8")}").map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("io.flow.delta.v0.models.Deployment", r, _.validate[io.flow.delta.v0.models.Deployment])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r if r.status == 404 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 404")
        }
      }
    }

    object Healthchecks extends Healthchecks {
      override def getHealthcheck()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.common.v0.models.Healthcheck] = {
        _executeRequest("GET", s"/_internal_/healthcheck").map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("io.flow.common.v0.models.Healthcheck", r, _.validate[io.flow.common.v0.models.Healthcheck])
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200")
        }
      }
    }

    object Images extends Images {
      override def get(
        projectId: String,
        id: _root_.scala.Option[String] = None,
        ids: _root_.scala.Option[Seq[String]] = None,
        limit: Long = 25,
        offset: Long = 0
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Image]] = {
        val queryParameters = Seq(
          id.map("id" -> _),
          Some("limit" -> limit.toString),
          Some("offset" -> offset.toString)
        ).flatten ++
          ids.getOrElse(Nil).map("ids" -> _)

        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(projectId, "UTF-8")}/images", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("Seq[io.flow.delta.v0.models.Image]", r, _.validate[Seq[io.flow.delta.v0.models.Image]])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r if r.status == 422 => throw new io.flow.delta.v0.errors.ErrorsResponse(r)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 422")
        }
      }
    }

    object Projects extends Projects {
      override def getVersions(
        id: _root_.scala.Option[String] = None,
        ids: _root_.scala.Option[Seq[String]] = None,
        organization: _root_.scala.Option[String] = None,
        limit: Long = 25,
        offset: Long = 0
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.ProjectVersion]] = {
        val queryParameters = Seq(
          id.map("id" -> _),
          organization.map("organization" -> _),
          Some("limit" -> limit.toString),
          Some("offset" -> offset.toString)
        ).flatten ++
          ids.getOrElse(Nil).map("ids" -> _)

        _executeRequest("GET", s"/projects/versions", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("Seq[io.flow.delta.v0.models.ProjectVersion]", r, _.validate[Seq[io.flow.delta.v0.models.ProjectVersion]])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
        }
      }

      override def get(
        id: _root_.scala.Option[String] = None,
        ids: _root_.scala.Option[Seq[String]] = None,
        organization: _root_.scala.Option[String] = None,
        limit: Long = 25,
        offset: Long = 0
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Project]] = {
        val queryParameters = Seq(
          id.map("id" -> _),
          organization.map("organization" -> _),
          Some("limit" -> limit.toString),
          Some("offset" -> offset.toString)
        ).flatten ++
          ids.getOrElse(Nil).map("ids" -> _)

        _executeRequest("GET", s"/projects", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("Seq[io.flow.delta.v0.models.Project]", r, _.validate[Seq[io.flow.delta.v0.models.Project]])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
        }
      }

      override def getById(
        id: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.delta.v0.models.Project] = {
        _executeRequest("GET", s"/projects/${play.utils.UriEncoding.encodePathSegment(id, "UTF-8")}").map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("io.flow.delta.v0.models.Project", r, _.validate[io.flow.delta.v0.models.Project])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r if r.status == 404 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 404")
        }
      }

      override def post(
        projectForm: io.flow.delta.v0.models.ProjectForm
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.delta.v0.models.Project] = {
        val payload = play.api.libs.json.Json.toJson(projectForm)

        _executeRequest("POST", s"/projects", body = Some(payload)).map {
          case r if r.status == 201 => _root_.io.flow.delta.v0.Client.parseJson("io.flow.delta.v0.models.Project", r, _.validate[io.flow.delta.v0.models.Project])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r if r.status == 422 => throw new io.flow.delta.v0.errors.ErrorsResponse(r)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 201, 401, 422")
        }
      }

      override def deleteById(
        id: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        _executeRequest("DELETE", s"/projects/${play.utils.UriEncoding.encodePathSegment(id, "UTF-8")}").map {
          case r if r.status == 204 => ()
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r if r.status == 404 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 204, 401, 404")
        }
      }
    }

    object Tags extends Tags {
      override def get(
        projectId: String,
        id: _root_.scala.Option[String] = None,
        ids: _root_.scala.Option[Seq[String]] = None,
        limit: Long = 25,
        offset: Long = 0
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Tag]] = {
        val queryParameters = Seq(
          id.map("id" -> _),
          Some("limit" -> limit.toString),
          Some("offset" -> offset.toString)
        ).flatten ++
          ids.getOrElse(Nil).map("ids" -> _)

        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(projectId, "UTF-8")}/tags", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.delta.v0.Client.parseJson("Seq[io.flow.delta.v0.models.Tag]", r, _.validate[Seq[io.flow.delta.v0.models.Tag]])
          case r if r.status == 401 => throw new io.flow.delta.v0.errors.UnitResponse(r.status)
          case r if r.status == 422 => throw new io.flow.delta.v0.errors.ErrorsResponse(r)
          case r => throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 422")
        }
      }
    }

    def _requestHolder(path: String): play.api.libs.ws.WSRequest = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path).withHeaders(
        "User-Agent" -> Constants.UserAgent,
        "X-Apidoc-Version" -> Constants.Version,
        "X-Apidoc-Version-Major" -> Constants.VersionMajor.toString
      ).withHeaders(defaultHeaders : _*)
      auth.fold(holder) {
        case Authorization.Basic(username, password) => {
          holder.withAuth(username, password.getOrElse(""), play.api.libs.ws.WSAuthScheme.BASIC)
        }
        case a => sys.error("Invalid authorization scheme[" + a.getClass + "]")
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequest)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequest = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield s"$name=$value"
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      auth.fold(logger.info(s"curl -X $method $url")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }
      req
    }

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).withQueryString(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).withQueryString(queryParameters:_*).withHeaders("Content-Type" -> "application/json; charset=UTF-8")).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).withQueryString(queryParameters:_*).withHeaders("Content-Type" -> "application/json; charset=UTF-8")).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).withQueryString(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).withQueryString(queryParameters:_*)).delete()
        }
         case "HEAD" => {
          _logRequest("HEAD", _requestHolder(path).withQueryString(queryParameters:_*)).head()
        }
         case "OPTIONS" => {
          _logRequest("OPTIONS", _requestHolder(path).withQueryString(queryParameters:_*)).options()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).withQueryString(queryParameters:_*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

  }

  object Client {

    def parseJson[T](
      className: String,
      r: play.api.libs.ws.WSResponse,
      f: (play.api.libs.json.JsValue => play.api.libs.json.JsResult[T])
    ): T = {
      f(play.api.libs.json.Json.parse(r.body)) match {
        case play.api.libs.json.JsSuccess(x, _) => x
        case play.api.libs.json.JsError(errors) => {
          throw new io.flow.delta.v0.errors.FailedRequest(r.status, s"Invalid json for class[" + className + "]: " + errors.mkString(" "))
        }
      }
    }

  }

  sealed trait Authorization
  object Authorization {
    case class Basic(username: String, password: Option[String] = None) extends Authorization
  }

  package interfaces {

    trait Client {
      def deployments: io.flow.delta.v0.Deployments
      def healthchecks: io.flow.delta.v0.Healthchecks
      def images: io.flow.delta.v0.Images
      def projects: io.flow.delta.v0.Projects
      def tags: io.flow.delta.v0.Tags
    }

  }

  trait Deployments {
    /**
     * Get a list of docker hub images for a project, with optional id(s)
     */
    def get(
      projectId: String,
      id: _root_.scala.Option[String] = None,
      ids: _root_.scala.Option[Seq[String]] = None,
      limit: Long = 25,
      offset: Long = 0
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Deployment]]

    /**
     * Get information about a specific deployment for this project
     */
    def getById(
      projectId: String,
      id: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.delta.v0.models.Deployment]
  }

  trait Healthchecks {
    def getHealthcheck()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.common.v0.models.Healthcheck]
  }

  trait Images {
    /**
     * Get a list of docker hub images for a project, with optional id(s)
     */
    def get(
      projectId: String,
      id: _root_.scala.Option[String] = None,
      ids: _root_.scala.Option[Seq[String]] = None,
      limit: Long = 25,
      offset: Long = 0
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Image]]
  }

  trait Projects {
    /**
     * Provides visibility into recent changes of each object, including deletion
     */
    def getVersions(
      id: _root_.scala.Option[String] = None,
      ids: _root_.scala.Option[Seq[String]] = None,
      organization: _root_.scala.Option[String] = None,
      limit: Long = 25,
      offset: Long = 0
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.ProjectVersion]]

    /**
     * Search projects. Results are paginated
     */
    def get(
      id: _root_.scala.Option[String] = None,
      ids: _root_.scala.Option[Seq[String]] = None,
      organization: _root_.scala.Option[String] = None,
      limit: Long = 25,
      offset: Long = 0
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Project]]

    /**
     * Returns information about this project.
     */
    def getById(
      id: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.delta.v0.models.Project]

    /**
     * Create a new project.
     */
    def post(
      projectForm: io.flow.delta.v0.models.ProjectForm
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.delta.v0.models.Project]

    def deleteById(
      id: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]
  }

  trait Tags {
    /**
     * Get a list of github tags for a project, with optional id(s)
     */
    def get(
      projectId: String,
      id: _root_.scala.Option[String] = None,
      ids: _root_.scala.Option[Seq[String]] = None,
      limit: Long = 25,
      offset: Long = 0
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.delta.v0.models.Tag]]
  }

  package errors {

    import io.flow.common.v0.models.json._
    import io.flow.delta.v0.models.json._

    case class ErrorsResponse(
      response: play.api.libs.ws.WSResponse,
      message: Option[String] = None
    ) extends Exception(message.getOrElse(response.status + ": " + response.body)){
      lazy val errors = _root_.io.flow.delta.v0.Client.parseJson("Seq[io.flow.common.v0.models.Error]", response, _.validate[Seq[io.flow.common.v0.models.Error]])
    }

    case class UnitResponse(status: Int) extends Exception(s"HTTP $status")

    case class FailedRequest(responseCode: Int, message: String, requestUri: Option[_root_.java.net.URI] = None) extends Exception(s"HTTP $responseCode: $message")

  }

}