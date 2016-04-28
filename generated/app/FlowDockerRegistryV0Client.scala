/**
 * Generated by apidoc - http://www.apidoc.me
 * Service version: 0.0.4
 * apidoc:0.11.24 http://www.apidoc.me/flow/docker-registry/0.0.4/play_2_4_client
 */
package io.flow.docker.registry.v0.models {

  case class Build(
    repoWebUrl: String
  )

  case class BuildForm(
    active: Boolean,
    buildTags: Seq[io.flow.docker.registry.v0.models.BuildTag],
    description: String,
    dockerhubRepoName: String,
    isPrivate: Boolean,
    name: String,
    namespace: String,
    provider: String,
    vcsRepoName: String
  )

  case class BuildTag(
    dockerfileLocation: String,
    name: String,
    sourceName: String,
    sourceType: String,
    id: _root_.scala.Option[Long] = None
  )

  case class Deploykey(
    provider: _root_.scala.Option[String] = None,
    providerKeyId: _root_.scala.Option[String] = None,
    publicKey: _root_.scala.Option[String] = None
  )

  case class DockerRepository(
    name: String
  )

  case class Tag(
    layer: String,
    name: String
  )

  case class V2Tag(
    results: Seq[io.flow.docker.registry.v0.models.V2TagDetails]
  )

  case class V2TagDetails(
    name: String
  )

}

package io.flow.docker.registry.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.flow.docker.registry.v0.models.json._

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

    implicit def jsonReadsDockerRegistryBuild: play.api.libs.json.Reads[Build] = {
      (__ \ "repo_web_url").read[String].map { x => new Build(repoWebUrl = x) }
    }

    def jsObjectBuild(obj: io.flow.docker.registry.v0.models.Build) = {
      play.api.libs.json.Json.obj(
        "repo_web_url" -> play.api.libs.json.JsString(obj.repoWebUrl)
      )
    }

    implicit def jsonWritesDockerRegistryBuild: play.api.libs.json.Writes[Build] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.Build] {
        def writes(obj: io.flow.docker.registry.v0.models.Build) = {
          jsObjectBuild(obj)
        }
      }
    }

    implicit def jsonReadsDockerRegistryBuildForm: play.api.libs.json.Reads[BuildForm] = {
      (
        (__ \ "active").read[Boolean] and
        (__ \ "build_tags").read[Seq[io.flow.docker.registry.v0.models.BuildTag]] and
        (__ \ "description").read[String] and
        (__ \ "dockerhub_repo_name").read[String] and
        (__ \ "is_private").read[Boolean] and
        (__ \ "name").read[String] and
        (__ \ "namespace").read[String] and
        (__ \ "provider").read[String] and
        (__ \ "vcs_repo_name").read[String]
      )(BuildForm.apply _)
    }

    def jsObjectBuildForm(obj: io.flow.docker.registry.v0.models.BuildForm) = {
      play.api.libs.json.Json.obj(
        "active" -> play.api.libs.json.JsBoolean(obj.active),
        "build_tags" -> play.api.libs.json.Json.toJson(obj.buildTags),
        "description" -> play.api.libs.json.JsString(obj.description),
        "dockerhub_repo_name" -> play.api.libs.json.JsString(obj.dockerhubRepoName),
        "is_private" -> play.api.libs.json.JsBoolean(obj.isPrivate),
        "name" -> play.api.libs.json.JsString(obj.name),
        "namespace" -> play.api.libs.json.JsString(obj.namespace),
        "provider" -> play.api.libs.json.JsString(obj.provider),
        "vcs_repo_name" -> play.api.libs.json.JsString(obj.vcsRepoName)
      )
    }

    implicit def jsonWritesDockerRegistryBuildForm: play.api.libs.json.Writes[BuildForm] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.BuildForm] {
        def writes(obj: io.flow.docker.registry.v0.models.BuildForm) = {
          jsObjectBuildForm(obj)
        }
      }
    }

    implicit def jsonReadsDockerRegistryBuildTag: play.api.libs.json.Reads[BuildTag] = {
      (
        (__ \ "dockerfile_location").read[String] and
        (__ \ "name").read[String] and
        (__ \ "source_name").read[String] and
        (__ \ "source_type").read[String] and
        (__ \ "id").readNullable[Long]
      )(BuildTag.apply _)
    }

    def jsObjectBuildTag(obj: io.flow.docker.registry.v0.models.BuildTag) = {
      play.api.libs.json.Json.obj(
        "dockerfile_location" -> play.api.libs.json.JsString(obj.dockerfileLocation),
        "name" -> play.api.libs.json.JsString(obj.name),
        "source_name" -> play.api.libs.json.JsString(obj.sourceName),
        "source_type" -> play.api.libs.json.JsString(obj.sourceType)
      ) ++ (obj.id match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("id" -> play.api.libs.json.JsNumber(x))
      })
    }

    implicit def jsonWritesDockerRegistryBuildTag: play.api.libs.json.Writes[BuildTag] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.BuildTag] {
        def writes(obj: io.flow.docker.registry.v0.models.BuildTag) = {
          jsObjectBuildTag(obj)
        }
      }
    }

    implicit def jsonReadsDockerRegistryDeploykey: play.api.libs.json.Reads[Deploykey] = {
      (
        (__ \ "provider").readNullable[String] and
        (__ \ "provider_key_id").readNullable[String] and
        (__ \ "public_key").readNullable[String]
      )(Deploykey.apply _)
    }

    def jsObjectDeploykey(obj: io.flow.docker.registry.v0.models.Deploykey) = {
      (obj.provider match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("provider" -> play.api.libs.json.JsString(x))
      }) ++
      (obj.providerKeyId match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("provider_key_id" -> play.api.libs.json.JsString(x))
      }) ++
      (obj.publicKey match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("public_key" -> play.api.libs.json.JsString(x))
      })
    }

    implicit def jsonWritesDockerRegistryDeploykey: play.api.libs.json.Writes[Deploykey] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.Deploykey] {
        def writes(obj: io.flow.docker.registry.v0.models.Deploykey) = {
          jsObjectDeploykey(obj)
        }
      }
    }

    implicit def jsonReadsDockerRegistryDockerRepository: play.api.libs.json.Reads[DockerRepository] = {
      (__ \ "name").read[String].map { x => new DockerRepository(name = x) }
    }

    def jsObjectDockerRepository(obj: io.flow.docker.registry.v0.models.DockerRepository) = {
      play.api.libs.json.Json.obj(
        "name" -> play.api.libs.json.JsString(obj.name)
      )
    }

    implicit def jsonWritesDockerRegistryDockerRepository: play.api.libs.json.Writes[DockerRepository] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.DockerRepository] {
        def writes(obj: io.flow.docker.registry.v0.models.DockerRepository) = {
          jsObjectDockerRepository(obj)
        }
      }
    }

    implicit def jsonReadsDockerRegistryTag: play.api.libs.json.Reads[Tag] = {
      (
        (__ \ "layer").read[String] and
        (__ \ "name").read[String]
      )(Tag.apply _)
    }

    def jsObjectTag(obj: io.flow.docker.registry.v0.models.Tag) = {
      play.api.libs.json.Json.obj(
        "layer" -> play.api.libs.json.JsString(obj.layer),
        "name" -> play.api.libs.json.JsString(obj.name)
      )
    }

    implicit def jsonWritesDockerRegistryTag: play.api.libs.json.Writes[Tag] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.Tag] {
        def writes(obj: io.flow.docker.registry.v0.models.Tag) = {
          jsObjectTag(obj)
        }
      }
    }

    implicit def jsonReadsDockerRegistryV2Tag: play.api.libs.json.Reads[V2Tag] = {
      (__ \ "results").read[Seq[io.flow.docker.registry.v0.models.V2TagDetails]].map { x => new V2Tag(results = x) }
    }

    def jsObjectV2Tag(obj: io.flow.docker.registry.v0.models.V2Tag) = {
      play.api.libs.json.Json.obj(
        "results" -> play.api.libs.json.Json.toJson(obj.results)
      )
    }

    implicit def jsonWritesDockerRegistryV2Tag: play.api.libs.json.Writes[V2Tag] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.V2Tag] {
        def writes(obj: io.flow.docker.registry.v0.models.V2Tag) = {
          jsObjectV2Tag(obj)
        }
      }
    }

    implicit def jsonReadsDockerRegistryV2TagDetails: play.api.libs.json.Reads[V2TagDetails] = {
      (__ \ "name").read[String].map { x => new V2TagDetails(name = x) }
    }

    def jsObjectV2TagDetails(obj: io.flow.docker.registry.v0.models.V2TagDetails) = {
      play.api.libs.json.Json.obj(
        "name" -> play.api.libs.json.JsString(obj.name)
      )
    }

    implicit def jsonWritesDockerRegistryV2TagDetails: play.api.libs.json.Writes[V2TagDetails] = {
      new play.api.libs.json.Writes[io.flow.docker.registry.v0.models.V2TagDetails] {
        def writes(obj: io.flow.docker.registry.v0.models.V2TagDetails) = {
          jsObjectV2TagDetails(obj)
        }
      }
    }
  }
}

package io.flow.docker.registry.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import io.flow.docker.registry.v0.models._

    // Type: date-time-iso8601
    implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    implicit val queryStringBindableTypeDateTimeIso8601 = new QueryStringBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    // Type: date-iso8601
    implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )

    implicit val queryStringBindableTypeDateIso8601 = new QueryStringBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )



  }

}


package io.flow.docker.registry.v0 {

  object Constants {

    val BaseUrl = "https://registry.hub.docker.com"
    val Namespace = "io.flow.docker.registry.v0"
    val UserAgent = "apidoc:0.11.24 http://www.apidoc.me/flow/docker-registry/0.0.4/play_2_4_client"
    val Version = "0.0.4"
    val VersionMajor = 0

  }

  class Client(
    val baseUrl: String = "https://registry.hub.docker.com",
    auth: scala.Option[io.flow.docker.registry.v0.Authorization] = None,
    defaultHeaders: Seq[(String, String)] = Nil
  ) extends interfaces.Client {
    import io.flow.docker.registry.v0.models.json._

    private[this] val logger = play.api.Logger("io.flow.docker.registry.v0.Client")

    logger.info(s"Initializing io.flow.docker.registry.v0.Client for url $baseUrl")

    def dockerRepositories: DockerRepositories = DockerRepositories

    def tags: Tags = Tags

    def v2Tags: V2Tags = V2Tags

    object DockerRepositories extends DockerRepositories {
      override def postAutobuild(
        org: String,
        repo: String,
        buildForm: io.flow.docker.registry.v0.models.BuildForm,
        requestHeaders: Seq[(String, String)] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.docker.registry.v0.models.Build] = {
        val payload = play.api.libs.json.Json.toJson(buildForm)

        _executeRequest("POST", s"/v2/repositories/${play.utils.UriEncoding.encodePathSegment(org, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(repo, "UTF-8")}/autobuild/", body = Some(payload), requestHeaders = requestHeaders).map {
          case r if r.status == 201 => _root_.io.flow.docker.registry.v0.Client.parseJson("io.flow.docker.registry.v0.models.Build", r, _.validate[io.flow.docker.registry.v0.models.Build])
          case r if r.status == 400 => throw new io.flow.docker.registry.v0.errors.UnitResponse(r.status)
          case r if r.status == 401 => throw new io.flow.docker.registry.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.docker.registry.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 201, 400, 401")
        }
      }
    }

    object Tags extends Tags {
      override def get(
        org: String,
        repo: String,
        requestHeaders: Seq[(String, String)] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.docker.registry.v0.models.Tag]] = {
        _executeRequest("GET", s"/v1/repositories/${play.utils.UriEncoding.encodePathSegment(org, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(repo, "UTF-8")}/tags", requestHeaders = requestHeaders).map {
          case r if r.status == 200 => _root_.io.flow.docker.registry.v0.Client.parseJson("Seq[io.flow.docker.registry.v0.models.Tag]", r, _.validate[Seq[io.flow.docker.registry.v0.models.Tag]])
          case r if r.status == 401 => throw new io.flow.docker.registry.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.docker.registry.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
        }
      }
    }

    object V2Tags extends V2Tags {
      override def get(
        org: String,
        repo: String,
        requestHeaders: Seq[(String, String)] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.docker.registry.v0.models.V2Tag] = {
        _executeRequest("GET", s"/v2/repositories/${play.utils.UriEncoding.encodePathSegment(org, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(repo, "UTF-8")}/tags", requestHeaders = requestHeaders).map {
          case r if r.status == 200 => _root_.io.flow.docker.registry.v0.Client.parseJson("io.flow.docker.registry.v0.models.V2Tag", r, _.validate[io.flow.docker.registry.v0.models.V2Tag])
          case r if r.status == 401 => throw new io.flow.docker.registry.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.docker.registry.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
        }
      }
    }

    def _requestHolder(path: String): play.api.libs.ws.WSRequest = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(baseUrl + path).withHeaders(
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
      queryParameters: Seq[(String, String)] = Nil,
      requestHeaders: Seq[(String, String)] = Nil,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).withHeaders(requestHeaders:_*).withQueryString(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).withHeaders(_withJsonContentType(requestHeaders):_*).withQueryString(queryParameters:_*)).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).withHeaders(_withJsonContentType(requestHeaders):_*).withQueryString(queryParameters:_*)).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).withHeaders(requestHeaders:_*).withQueryString(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).withHeaders(requestHeaders:_*).withQueryString(queryParameters:_*)).delete()
        }
         case "HEAD" => {
          _logRequest("HEAD", _requestHolder(path).withHeaders(requestHeaders:_*).withQueryString(queryParameters:_*)).head()
        }
         case "OPTIONS" => {
          _logRequest("OPTIONS", _requestHolder(path).withHeaders(requestHeaders:_*).withQueryString(queryParameters:_*)).options()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).withHeaders(requestHeaders:_*).withQueryString(queryParameters:_*))
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
          throw new io.flow.docker.registry.v0.errors.FailedRequest(r.status, s"Invalid json for class[" + className + "]: " + errors.mkString(" "))
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
      def baseUrl: String
      def dockerRepositories: io.flow.docker.registry.v0.DockerRepositories
      def tags: io.flow.docker.registry.v0.Tags
      def v2Tags: io.flow.docker.registry.v0.V2Tags
    }

  }

  trait DockerRepositories {
    def postAutobuild(
      org: String,
      repo: String,
      buildForm: io.flow.docker.registry.v0.models.BuildForm,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.docker.registry.v0.models.Build]
  }

  trait Tags {
    def get(
      org: String,
      repo: String,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.docker.registry.v0.models.Tag]]
  }

  trait V2Tags {
    def get(
      org: String,
      repo: String,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.docker.registry.v0.models.V2Tag]
  }

  package errors {

    import io.flow.docker.registry.v0.models.json._

    case class UnitResponse(status: Int) extends Exception(s"HTTP $status")

    case class FailedRequest(responseCode: Int, message: String, requestUri: Option[_root_.java.net.URI] = None) extends _root_.java.lang.Exception(s"HTTP $responseCode: $message")

  }

}