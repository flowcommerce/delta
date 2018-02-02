package controllers

import io.flow.delta.v0.Client
import io.flow.delta.v0.models.UserForm
import io.flow.common.v0.models.Name
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class UsersSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val user1 = createUser()
  lazy val user2 = createUser()

  "GET /users requires auth" in new WithServer(port=port) {
    expectNotAuthorized {
      anonClient.users.get()
    }
  }

  "GET /users/:id" in new WithServer(port=port) {
    expectNotAuthorized {
      anonClient.users.getById(UUID.randomUUID.toString)
    }
  }

  "GET /users by id" in new WithServer(port=port) {
    await(
      client.users.get(id = Some(user1.id))
    ).map(_.id) must beEqualTo(
      Seq(user1.id)
    )

    await(
      client.users.get(id = Some(UUID.randomUUID.toString))
    ).map(_.id) must be(
      Nil
    )
  }

  "GET /users by email" in new WithServer(port=port) {
    await(
      client.users.get(email = user1.email)
    ).map(_.email) must beEqualTo(
      Seq(user1.email)
    )

    await(
      client.users.get(email = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /users/:id" in new WithServer(port=port) {
    await(client.users.getById(user1.id)).id must beEqualTo(user1.id)
    await(client.users.getById(user2.id)).id must beEqualTo(user2.id)

    expectNotFound {
      client.users.getById(UUID.randomUUID.toString)
    }
  }

  "POST /users w/out name" in new WithServer(port=port) {
    val email = createTestEmail()
    val user = await(anonClient.users.post(UserForm(email = Some(email))))
    user.email must beEqualTo(Some(email))
    user.name.first must beEqualTo(None)
    user.name.last must beEqualTo(None)
  }

  "POST /users w/ name" in new WithServer(port=port) {
    val email = createTestEmail()
    val user = await(
      anonClient.users.post(
        UserForm(
          email = Some(email),
          name = Some(
            Name(first = Some("Michael"), last = Some("Bryzek"))
          )
        )
      )
    )
    user.email must beEqualTo(Some(email))
    user.name.first must beEqualTo(Some("Michael"))
    user.name.last must beEqualTo(Some("Bryzek"))
  }

  "POST /users validates duplicate email" in new WithServer(port=port) {
    expectErrors(
      anonClient.users.post(UserForm(email = Some(user1.email.get)))
    ).errors.map(_.message) must beEqualTo(
      Seq("Email is already registered")
    )
  }

  "POST /users validates empty email" in new WithServer(port=port) {
    expectErrors(
      anonClient.users.post(UserForm(email = Some("   ")))
    ).errors.map(_.message) must beEqualTo(
      Seq("Email address cannot be empty")
    )
  }

  "POST /users validates email address format" in new WithServer(port=port) {
    expectErrors(
      anonClient.users.post(UserForm(email = Some("mbfoo.com")))
    ).errors.map(_.message) must beEqualTo(
      Seq("Please enter a valid email address")
    )
  }

}
