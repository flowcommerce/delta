/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.2.10
 * apibuilder 0.14.93 app.apibuilder.io/flow/rollbar/latest/play_2_8_client
 */
package io.flow.rollbar.v0.models {

  final case class Deploy(
    accessToken: String,
    environment: String,
    revision: String,
    rollbarUsername: _root_.scala.Option[String] = None,
    localUsername: _root_.scala.Option[String] = None,
    comment: _root_.scala.Option[String] = None
  )

  final case class Error(
    err: Int,
    message: String
  )

  final case class Project(
    id: Int,
    name: _root_.scala.Option[String] = None
  )

  final case class ProjectAccessToken(
    projectId: Int,
    accessToken: String,
    scopes: Seq[String]
  )

  final case class ProjectAccessTokensResult(
    err: Int,
    result: Seq[io.flow.rollbar.v0.models.ProjectAccessToken]
  )

  final case class ProjectsResult(
    err: Int,
    result: Seq[io.flow.rollbar.v0.models.Project]
  )

}

package io.flow.rollbar.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.flow.rollbar.v0.models.json._

    private[v0] implicit val jsonReadsUUID = __.read[String].map { str =>
      _root_.java.util.UUID.fromString(str)
    }

    private[v0] implicit val jsonWritesUUID = new Writes[_root_.java.util.UUID] {
      def writes(x: _root_.java.util.UUID) = JsString(x.toString)
    }

    private[v0] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseDateTime(str)
    }

    private[v0] implicit val jsonWritesJodaDateTime = new Writes[_root_.org.joda.time.DateTime] {
      def writes(x: _root_.org.joda.time.DateTime) = {
        JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(x))
      }
    }

    private[v0] implicit val jsonReadsJodaLocalDate = __.read[String].map { str =>
      _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseLocalDate(str)
    }

    private[v0] implicit val jsonWritesJodaLocalDate = new Writes[_root_.org.joda.time.LocalDate] {
      def writes(x: _root_.org.joda.time.LocalDate) = {
        JsString(_root_.org.joda.time.format.ISODateTimeFormat.date.print(x))
      }
    }

    implicit def jsonReadsRollbarDeploy: play.api.libs.json.Reads[Deploy] = {
      for {
        accessToken <- (__ \ "access_token").read[String]
        environment <- (__ \ "environment").read[String]
        revision <- (__ \ "revision").read[String]
        rollbarUsername <- (__ \ "rollbar_username").readNullable[String]
        localUsername <- (__ \ "local_username").readNullable[String]
        comment <- (__ \ "comment").readNullable[String]
      } yield Deploy(accessToken, environment, revision, rollbarUsername, localUsername, comment)
    }

    def jsObjectDeploy(obj: io.flow.rollbar.v0.models.Deploy): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "access_token" -> play.api.libs.json.JsString(obj.accessToken),
        "environment" -> play.api.libs.json.JsString(obj.environment),
        "revision" -> play.api.libs.json.JsString(obj.revision)
      ) ++ (obj.rollbarUsername match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("rollbar_username" -> play.api.libs.json.JsString(x))
      }) ++
      (obj.localUsername match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("local_username" -> play.api.libs.json.JsString(x))
      }) ++
      (obj.comment match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("comment" -> play.api.libs.json.JsString(x))
      })
    }

    implicit def jsonWritesRollbarDeploy: play.api.libs.json.Writes[Deploy] = {
      new play.api.libs.json.Writes[io.flow.rollbar.v0.models.Deploy] {
        def writes(obj: io.flow.rollbar.v0.models.Deploy) = {
          jsObjectDeploy(obj)
        }
      }
    }

    implicit def jsonReadsRollbarError: play.api.libs.json.Reads[Error] = {
      for {
        err <- (__ \ "err").read[Int]
        message <- (__ \ "message").read[String]
      } yield Error(err, message)
    }

    def jsObjectError(obj: io.flow.rollbar.v0.models.Error): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "err" -> play.api.libs.json.JsNumber(obj.err),
        "message" -> play.api.libs.json.JsString(obj.message)
      )
    }

    implicit def jsonWritesRollbarError: play.api.libs.json.Writes[Error] = {
      new play.api.libs.json.Writes[io.flow.rollbar.v0.models.Error] {
        def writes(obj: io.flow.rollbar.v0.models.Error) = {
          jsObjectError(obj)
        }
      }
    }

    implicit def jsonReadsRollbarProject: play.api.libs.json.Reads[Project] = {
      for {
        id <- (__ \ "id").read[Int]
        name <- (__ \ "name").readNullable[String]
      } yield Project(id, name)
    }

    def jsObjectProject(obj: io.flow.rollbar.v0.models.Project): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsNumber(obj.id)
      ) ++ (obj.name match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("name" -> play.api.libs.json.JsString(x))
      })
    }

    implicit def jsonWritesRollbarProject: play.api.libs.json.Writes[Project] = {
      new play.api.libs.json.Writes[io.flow.rollbar.v0.models.Project] {
        def writes(obj: io.flow.rollbar.v0.models.Project) = {
          jsObjectProject(obj)
        }
      }
    }

    implicit def jsonReadsRollbarProjectAccessToken: play.api.libs.json.Reads[ProjectAccessToken] = {
      for {
        projectId <- (__ \ "project_id").read[Int]
        accessToken <- (__ \ "access_token").read[String]
        scopes <- (__ \ "scopes").read[Seq[String]]
      } yield ProjectAccessToken(projectId, accessToken, scopes)
    }

    def jsObjectProjectAccessToken(obj: io.flow.rollbar.v0.models.ProjectAccessToken): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "project_id" -> play.api.libs.json.JsNumber(obj.projectId),
        "access_token" -> play.api.libs.json.JsString(obj.accessToken),
        "scopes" -> play.api.libs.json.Json.toJson(obj.scopes)
      )
    }

    implicit def jsonWritesRollbarProjectAccessToken: play.api.libs.json.Writes[ProjectAccessToken] = {
      new play.api.libs.json.Writes[io.flow.rollbar.v0.models.ProjectAccessToken] {
        def writes(obj: io.flow.rollbar.v0.models.ProjectAccessToken) = {
          jsObjectProjectAccessToken(obj)
        }
      }
    }

    implicit def jsonReadsRollbarProjectAccessTokensResult: play.api.libs.json.Reads[ProjectAccessTokensResult] = {
      for {
        err <- (__ \ "err").read[Int]
        result <- (__ \ "result").read[Seq[io.flow.rollbar.v0.models.ProjectAccessToken]]
      } yield ProjectAccessTokensResult(err, result)
    }

    def jsObjectProjectAccessTokensResult(obj: io.flow.rollbar.v0.models.ProjectAccessTokensResult): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "err" -> play.api.libs.json.JsNumber(obj.err),
        "result" -> play.api.libs.json.Json.toJson(obj.result)
      )
    }

    implicit def jsonWritesRollbarProjectAccessTokensResult: play.api.libs.json.Writes[ProjectAccessTokensResult] = {
      new play.api.libs.json.Writes[io.flow.rollbar.v0.models.ProjectAccessTokensResult] {
        def writes(obj: io.flow.rollbar.v0.models.ProjectAccessTokensResult) = {
          jsObjectProjectAccessTokensResult(obj)
        }
      }
    }

    implicit def jsonReadsRollbarProjectsResult: play.api.libs.json.Reads[ProjectsResult] = {
      for {
        err <- (__ \ "err").read[Int]
        result <- (__ \ "result").read[Seq[io.flow.rollbar.v0.models.Project]]
      } yield ProjectsResult(err, result)
    }

    def jsObjectProjectsResult(obj: io.flow.rollbar.v0.models.ProjectsResult): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "err" -> play.api.libs.json.JsNumber(obj.err),
        "result" -> play.api.libs.json.Json.toJson(obj.result)
      )
    }

    implicit def jsonWritesRollbarProjectsResult: play.api.libs.json.Writes[ProjectsResult] = {
      new play.api.libs.json.Writes[io.flow.rollbar.v0.models.ProjectsResult] {
        def writes(obj: io.flow.rollbar.v0.models.ProjectsResult) = {
          jsObjectProjectsResult(obj)
        }
      }
    }
  }
}

package io.flow.rollbar.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}

    // import models directly for backwards compatibility with prior versions of the generator
    import Core._

    object Core {
      implicit def pathBindableDateTimeIso8601(implicit stringBinder: QueryStringBindable[String]): PathBindable[_root_.org.joda.time.DateTime] = ApibuilderPathBindable(ApibuilderTypes.dateTimeIso8601)
      implicit def queryStringBindableDateTimeIso8601(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[_root_.org.joda.time.DateTime] = ApibuilderQueryStringBindable(ApibuilderTypes.dateTimeIso8601)

      implicit def pathBindableDateIso8601(implicit stringBinder: QueryStringBindable[String]): PathBindable[_root_.org.joda.time.LocalDate] = ApibuilderPathBindable(ApibuilderTypes.dateIso8601)
      implicit def queryStringBindableDateIso8601(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[_root_.org.joda.time.LocalDate] = ApibuilderQueryStringBindable(ApibuilderTypes.dateIso8601)
    }

    trait ApibuilderTypeConverter[T] {

      def convert(value: String): T

      def convert(value: T): String

      def example: T

      def validValues: Seq[T] = Nil

      def errorMessage(key: String, value: String, ex: java.lang.Exception): String = {
        val base = s"Invalid value '$value' for parameter '$key'. "
        validValues.toList match {
          case Nil => base + "Ex: " + convert(example)
          case values => base + ". Valid values are: " + values.mkString("'", "', '", "'")
        }
      }
    }

    object ApibuilderTypes {
      val dateTimeIso8601: ApibuilderTypeConverter[_root_.org.joda.time.DateTime] = new ApibuilderTypeConverter[_root_.org.joda.time.DateTime] {
        override def convert(value: String): _root_.org.joda.time.DateTime = _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseDateTime(value)
        override def convert(value: _root_.org.joda.time.DateTime): String = _root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(value)
        override def example: _root_.org.joda.time.DateTime = _root_.org.joda.time.DateTime.now
      }

      val dateIso8601: ApibuilderTypeConverter[_root_.org.joda.time.LocalDate] = new ApibuilderTypeConverter[_root_.org.joda.time.LocalDate] {
        override def convert(value: String): _root_.org.joda.time.LocalDate = _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseLocalDate(value)
        override def convert(value: _root_.org.joda.time.LocalDate): String = _root_.org.joda.time.format.ISODateTimeFormat.date.print(value)
        override def example: _root_.org.joda.time.LocalDate = _root_.org.joda.time.LocalDate.now
      }
    }

    final case class ApibuilderQueryStringBindable[T](
      converters: ApibuilderTypeConverter[T]
    ) extends QueryStringBindable[T] {

      override def bind(key: String, params: Map[String, Seq[String]]): _root_.scala.Option[_root_.scala.Either[String, T]] = {
        params.getOrElse(key, Nil).headOption.map { v =>
          try {
            Right(
              converters.convert(v)
            )
          } catch {
            case ex: java.lang.Exception => Left(
              converters.errorMessage(key, v, ex)
            )
          }
        }
      }

      override def unbind(key: String, value: T): String = {
        s"$key=${converters.convert(value)}"
      }
    }

    final case class ApibuilderPathBindable[T](
      converters: ApibuilderTypeConverter[T]
    ) extends PathBindable[T] {

      override def bind(key: String, value: String): _root_.scala.Either[String, T] = {
        try {
          Right(
            converters.convert(value)
          )
        } catch {
          case ex: java.lang.Exception => Left(
            converters.errorMessage(key, value, ex)
          )
        }
      }

      override def unbind(key: String, value: T): String = {
        converters.convert(value)
      }
    }

  }

}


package io.flow.rollbar.v0 {

  object Constants {

    val BaseUrl = "https://api.rollbar.com/api/1"
    val Namespace = "io.flow.rollbar.v0"
    val UserAgent = "apibuilder 0.14.93 app.apibuilder.io/flow/rollbar/latest/play_2_8_client"
    val Version = "0.2.10"
    val VersionMajor = 0

  }

  class Client(
    ws: play.api.libs.ws.WSClient,
    val baseUrl: String = "https://api.rollbar.com/api/1",
    auth: scala.Option[io.flow.rollbar.v0.Authorization] = None,
    defaultHeaders: Seq[(String, String)] = Nil
  ) extends interfaces.Client {
    import io.flow.rollbar.v0.models.json._

    private[this] val logger = play.api.Logger("io.flow.rollbar.v0.Client")

    logger.info(s"Initializing io.flow.rollbar.v0.Client for url $baseUrl")

    def deploys: Deploys = Deploys

    def projects: Projects = Projects

    object Deploys extends Deploys {
      override def post(
        deploy: io.flow.rollbar.v0.models.Deploy,
        requestHeaders: Seq[(String, String)] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        val payload = play.api.libs.json.Json.toJson(deploy)

        _executeRequest("POST", s"/deploy", body = Some(payload), requestHeaders = requestHeaders).map {
          case r if r.status == 200 => ()
          case r => throw io.flow.rollbar.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200")
        }
      }
    }

    object Projects extends Projects {
      override def getProjects(
        accessToken: String,
        requestHeaders: Seq[(String, String)] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.rollbar.v0.models.ProjectsResult] = {
        val queryParameters = Seq(
          Some("access_token" -> accessToken)
        ).flatten

        _executeRequest("GET", s"/projects", queryParameters = queryParameters, requestHeaders = requestHeaders).map {
          case r if r.status == 200 => _root_.io.flow.rollbar.v0.Client.parseJson("io.flow.rollbar.v0.models.ProjectsResult", r, _.validate[io.flow.rollbar.v0.models.ProjectsResult])
          case r if r.status == 401 => throw io.flow.rollbar.v0.errors.ErrorResponse(r)
          case r => throw io.flow.rollbar.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
        }
      }

      override def getProjectAndAccessTokensByProjectId(
        projectId: Int,
        accessToken: String,
        requestHeaders: Seq[(String, String)] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.rollbar.v0.models.ProjectAccessTokensResult] = {
        val queryParameters = Seq(
          Some("access_token" -> accessToken)
        ).flatten

        _executeRequest("GET", s"/project/${projectId}/access_tokens", queryParameters = queryParameters, requestHeaders = requestHeaders).map {
          case r if r.status == 200 => _root_.io.flow.rollbar.v0.Client.parseJson("io.flow.rollbar.v0.models.ProjectAccessTokensResult", r, _.validate[io.flow.rollbar.v0.models.ProjectAccessTokensResult])
          case r if r.status == 401 => throw io.flow.rollbar.v0.errors.ErrorResponse(r)
          case r if r.status == 403 => throw io.flow.rollbar.v0.errors.ErrorResponse(r)
          case r => throw io.flow.rollbar.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 403")
        }
      }
    }

    def _requestHolder(path: String): play.api.libs.ws.WSRequest = {

      val holder = ws.url(baseUrl + path).addHttpHeaders(
        "User-Agent" -> Constants.UserAgent,
        "X-Apidoc-Version" -> Constants.Version,
        "X-Apidoc-Version-Major" -> Constants.VersionMajor.toString
      ).addHttpHeaders(defaultHeaders : _*)
      auth.fold(holder) {
        case Authorization.Basic(username, password) => {
          holder.withAuth(username, password.getOrElse(""), play.api.libs.ws.WSAuthScheme.BASIC)
        }
        case a => sys.error("Invalid authorization scheme[" + a.getClass + "]")
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequest): play.api.libs.ws.WSRequest = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield s"$name=$value"
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      auth.fold(logger.info(s"curl -X $method '$url'")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' '$url'")
      }
      req
    }

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Nil,
      requestHeaders: Seq[(String, String)] = Nil,
      body: Option[play.api.libs.json.JsValue] = None
    ): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).addHttpHeaders(requestHeaders:_*).addQueryStringParameters(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).addHttpHeaders(_withJsonContentType(requestHeaders):_*).addQueryStringParameters(queryParameters:_*)).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).addHttpHeaders(_withJsonContentType(requestHeaders):_*).addQueryStringParameters(queryParameters:_*)).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).addHttpHeaders(requestHeaders:_*).addQueryStringParameters(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).addHttpHeaders(requestHeaders:_*).addQueryStringParameters(queryParameters:_*)).delete()
        }
         case "HEAD" => {
          _logRequest("HEAD", _requestHolder(path).addHttpHeaders(requestHeaders:_*).addQueryStringParameters(queryParameters:_*)).head()
        }
         case "OPTIONS" => {
          _logRequest("OPTIONS", _requestHolder(path).addHttpHeaders(requestHeaders:_*).addQueryStringParameters(queryParameters:_*)).options()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).addHttpHeaders(requestHeaders:_*).addQueryStringParameters(queryParameters:_*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

    /**
     * Adds a Content-Type: application/json header unless the specified requestHeaders
     * already contain a Content-Type header
     */
    def _withJsonContentType(headers: Seq[(String, String)]): Seq[(String, String)] = {
      headers.find { _._1.toUpperCase == "CONTENT-TYPE" } match {
        case None => headers ++ Seq(("Content-Type" -> "application/json; charset=UTF-8"))
        case Some(_) => headers
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
          throw io.flow.rollbar.v0.errors.FailedRequest(r.status, s"Invalid json for class[" + className + "]: " + errors.mkString(" "))
        }
      }
    }

  }

  sealed trait Authorization extends _root_.scala.Product with _root_.scala.Serializable
  object Authorization {
    final case class Basic(username: String, password: Option[String] = None) extends Authorization
  }

  package interfaces {

    trait Client {
      def baseUrl: String
      def deploys: io.flow.rollbar.v0.Deploys
      def projects: io.flow.rollbar.v0.Projects
    }

  }

  trait Deploys {
    def post(
      deploy: io.flow.rollbar.v0.models.Deploy,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]
  }

  trait Projects {
    def getProjects(
      accessToken: String,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.rollbar.v0.models.ProjectsResult]

    def getProjectAndAccessTokensByProjectId(
      projectId: Int,
      accessToken: String,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.rollbar.v0.models.ProjectAccessTokensResult]
  }

  package errors {

    import io.flow.rollbar.v0.models.json._

    final case class ErrorResponse(
      response: play.api.libs.ws.WSResponse,
      message: Option[String] = None
    ) extends Exception(message.getOrElse(response.status + ": " + response.body)){
      lazy val error = _root_.io.flow.rollbar.v0.Client.parseJson("io.flow.rollbar.v0.models.Error", response, _.validate[io.flow.rollbar.v0.models.Error])
    }

    final case class FailedRequest(responseCode: Int, message: String, requestUri: Option[_root_.java.net.URI] = None) extends _root_.java.lang.Exception(s"HTTP $responseCode: $message")

  }

}