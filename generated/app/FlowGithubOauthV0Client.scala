/**
 * Generated by apidoc - http://www.apidoc.me
 * Service version: 0.0.3
 * apidoc:0.11.38 http://www.apidoc.me/flow/github-oauth/0.0.3/play_2_4_client
 */
package io.flow.github.oauth.v0.models {

  case class AccessToken(
    accessToken: String,
    scope: String,
    tokenType: io.flow.github.oauth.v0.models.TokenType
  )

  case class AccessTokenForm(
    clientId: String,
    clientSecret: String,
    code: String,
    redirectUri: _root_.scala.Option[String] = None,
    state: _root_.scala.Option[String] = None
  )

  sealed trait TokenType

  object TokenType {

    case object Bearer extends TokenType { override def toString = "bearer" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends TokenType

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Bearer)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): TokenType = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[TokenType] = byName.get(value.toLowerCase)

  }

}

package io.flow.github.oauth.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.flow.github.oauth.v0.models.json._

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

    implicit val jsonReadsGithubOauthTokenType = new play.api.libs.json.Reads[io.flow.github.oauth.v0.models.TokenType] {
      def reads(js: play.api.libs.json.JsValue): play.api.libs.json.JsResult[io.flow.github.oauth.v0.models.TokenType] = {
        js match {
          case v: play.api.libs.json.JsString => play.api.libs.json.JsSuccess(io.flow.github.oauth.v0.models.TokenType(v.value))
          case _ => {
            (js \ "value").validate[String] match {
              case play.api.libs.json.JsSuccess(v, _) => play.api.libs.json.JsSuccess(io.flow.github.oauth.v0.models.TokenType(v))
              case err: play.api.libs.json.JsError => err
            }
          }
        }
      }
    }

    def jsonWritesGithubOauthTokenType(obj: io.flow.github.oauth.v0.models.TokenType) = {
      play.api.libs.json.JsString(obj.toString)
    }

    def jsObjectTokenType(obj: io.flow.github.oauth.v0.models.TokenType) = {
      play.api.libs.json.Json.obj("value" -> play.api.libs.json.JsString(obj.toString))
    }

    implicit def jsonWritesGithubOauthTokenType: play.api.libs.json.Writes[TokenType] = {
      new play.api.libs.json.Writes[io.flow.github.oauth.v0.models.TokenType] {
        def writes(obj: io.flow.github.oauth.v0.models.TokenType) = {
          jsonWritesGithubOauthTokenType(obj)
        }
      }
    }

    implicit def jsonReadsGithubOauthAccessToken: play.api.libs.json.Reads[AccessToken] = {
      (
        (__ \ "access_token").read[String] and
        (__ \ "scope").read[String] and
        (__ \ "token_type").read[io.flow.github.oauth.v0.models.TokenType]
      )(AccessToken.apply _)
    }

    def jsObjectAccessToken(obj: io.flow.github.oauth.v0.models.AccessToken) = {
      play.api.libs.json.Json.obj(
        "access_token" -> play.api.libs.json.JsString(obj.accessToken),
        "scope" -> play.api.libs.json.JsString(obj.scope),
        "token_type" -> play.api.libs.json.JsString(obj.tokenType.toString)
      )
    }

    implicit def jsonWritesGithubOauthAccessToken: play.api.libs.json.Writes[AccessToken] = {
      new play.api.libs.json.Writes[io.flow.github.oauth.v0.models.AccessToken] {
        def writes(obj: io.flow.github.oauth.v0.models.AccessToken) = {
          jsObjectAccessToken(obj)
        }
      }
    }

    implicit def jsonReadsGithubOauthAccessTokenForm: play.api.libs.json.Reads[AccessTokenForm] = {
      (
        (__ \ "client_id").read[String] and
        (__ \ "client_secret").read[String] and
        (__ \ "code").read[String] and
        (__ \ "redirect_uri").readNullable[String] and
        (__ \ "state").readNullable[String]
      )(AccessTokenForm.apply _)
    }

    def jsObjectAccessTokenForm(obj: io.flow.github.oauth.v0.models.AccessTokenForm) = {
      play.api.libs.json.Json.obj(
        "client_id" -> play.api.libs.json.JsString(obj.clientId),
        "client_secret" -> play.api.libs.json.JsString(obj.clientSecret),
        "code" -> play.api.libs.json.JsString(obj.code)
      ) ++ (obj.redirectUri match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("redirect_uri" -> play.api.libs.json.JsString(x))
      }) ++
      (obj.state match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("state" -> play.api.libs.json.JsString(x))
      })
    }

    implicit def jsonWritesGithubOauthAccessTokenForm: play.api.libs.json.Writes[AccessTokenForm] = {
      new play.api.libs.json.Writes[io.flow.github.oauth.v0.models.AccessTokenForm] {
        def writes(obj: io.flow.github.oauth.v0.models.AccessTokenForm) = {
          jsObjectAccessTokenForm(obj)
        }
      }
    }
  }
}

package io.flow.github.oauth.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import io.flow.github.oauth.v0.models._

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

    // Enum: TokenType
    private[this] val enumTokenTypeNotFound = (key: String, e: _root_.java.lang.Exception) => s"Unrecognized $key, should be one of ${io.flow.github.oauth.v0.models.TokenType.all.mkString(", ")}"

    implicit val pathBindableEnumTokenType = new PathBindable.Parsing[io.flow.github.oauth.v0.models.TokenType] (
      TokenType.fromString(_).get, _.toString, enumTokenTypeNotFound
    )

    implicit val queryStringBindableEnumTokenType = new QueryStringBindable.Parsing[io.flow.github.oauth.v0.models.TokenType](
      TokenType.fromString(_).get, _.toString, enumTokenTypeNotFound
    )

  }

}


package io.flow.github.oauth.v0 {

  object Constants {

    val BaseUrl = "https://github.com"
    val Namespace = "io.flow.github.oauth.v0"
    val UserAgent = "apidoc:0.11.38 http://www.apidoc.me/flow/github-oauth/0.0.3/play_2_4_client"
    val Version = "0.0.3"
    val VersionMajor = 0

  }

  class Client(
    val baseUrl: String = "https://github.com",
    auth: scala.Option[io.flow.github.oauth.v0.Authorization] = None,
    defaultHeaders: Seq[(String, String)] = Nil
  ) extends interfaces.Client {
    import io.flow.github.oauth.v0.models.json._

    private[this] val logger = play.api.Logger("io.flow.github.oauth.v0.Client")

    logger.info(s"Initializing io.flow.github.oauth.v0.Client for url $baseUrl")

    def accessTokens: AccessTokens = AccessTokens

    object AccessTokens extends AccessTokens {
      override def postAccessToken(
        accessTokenForm: io.flow.github.oauth.v0.models.AccessTokenForm,
        requestHeaders: Seq[(String, String)] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.github.oauth.v0.models.AccessToken] = {
        val payload = play.api.libs.json.Json.toJson(accessTokenForm)

        _executeRequest("POST", s"/login/oauth/access_token", body = Some(payload), requestHeaders = requestHeaders).map {
          case r if r.status == 200 => _root_.io.flow.github.oauth.v0.Client.parseJson("io.flow.github.oauth.v0.models.AccessToken", r, _.validate[io.flow.github.oauth.v0.models.AccessToken])
          case r if r.status == 401 => throw new io.flow.github.oauth.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.github.oauth.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
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
          throw new io.flow.github.oauth.v0.errors.FailedRequest(r.status, s"Invalid json for class[" + className + "]: " + errors.mkString(" "))
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
      def accessTokens: io.flow.github.oauth.v0.AccessTokens
    }

  }

  trait AccessTokens {
    def postAccessToken(
      accessTokenForm: io.flow.github.oauth.v0.models.AccessTokenForm,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.github.oauth.v0.models.AccessToken]
  }

  package errors {

    import io.flow.github.oauth.v0.models.json._

    case class UnitResponse(status: Int) extends Exception(s"HTTP $status")

    case class FailedRequest(responseCode: Int, message: String, requestUri: Option[_root_.java.net.URI] = None) extends _root_.java.lang.Exception(s"HTTP $responseCode: $message")

  }

}