package io.flow.delta.api.lib

import db.{TokensDao, UsersDao}
import io.flow.common.v0.models.UserReference
import io.flow.token.v0.interfaces.Client
import io.flow.token.v0.errors.UnitResponse
import io.flow.token.v0.models.{TokenAuthenticationForm, TokenReference, Token => FlowToken}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class DefaultTokenClient() extends Client {

  def baseUrl = throw new UnsupportedOperationException()

  def tokens: io.flow.token.v0.Tokens = new Tokens()

  def validations = throw new UnsupportedOperationException()

}

class Tokens() extends io.flow.token.v0.Tokens {

  override def getVersions(
    id: _root_.scala.Option[Seq[String]] = None,
    tokenId: _root_.scala.Option[Seq[String]] = None,
    limit: Long = 25,
    offset: Long = 0,
    sort: String = "journal_timestamp",
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.token.v0.models.TokenVersion]] = throw new UnsupportedOperationException()

  override def getCleartextById(
    id: String,
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.token.v0.models.Cleartext] = throw new UnsupportedOperationException()

  override def get(
    id: _root_.scala.Option[Seq[String]] = None,
    token: _root_.scala.Option[String] = None,
    limit: Long = 25,
    offset: Long = 0,
    sort: String = "-created_at",
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.token.v0.models.Token]] = Future {
    token.flatMap { t =>
      TokensDao.findByToken(t) match {
        case Some(t) => {
          Some(FlowToken(id = t.id, user = UserReference(t.user.id), createdAt = new DateTime, partial = t.id))
        }

        case None => {
          UsersDao.findById(t).map { u => FlowToken(id = u.id, user = UserReference(id = u.id), createdAt = new DateTime, partial = u.id) }
        }
      }
    }.toSeq
  }

  override def getById(
    token: String,
    requestHeaders: Seq[(String, String)]
  )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.token.v0.models.Token] = {
    get(token = Some(token), requestHeaders = requestHeaders).map(_.headOption.getOrElse {
      throw new UnitResponse(404)
    })
  }

  override def post(
    tokenForm: io.flow.token.v0.models.TokenForm,
    requestHeaders: Seq[(String, String)]
  )(implicit ec: scala.concurrent.ExecutionContext) = throw new UnsupportedOperationException()

  override def deleteById(
    token: String,
    requestHeaders: Seq[(String, String)]
  )(implicit ec: scala.concurrent.ExecutionContext) = throw new UnsupportedOperationException()

  override def postAuthentications(authenticationForm: TokenAuthenticationForm, requestHeaders: Seq[(String, String)])(implicit ec: ExecutionContext): Future[TokenReference] = ???
}
