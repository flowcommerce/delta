package controllers

import java.util.UUID

import io.flow.common.v0.models.Name
import io.flow.delta.v0.models.UserForm

class UsersSpec extends MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val user1 = createUser()
  lazy val user2 = createUser()

  "GET /users requires auth" in {
    expectNotAuthorized {
      anonClient.users.get()
    }
  }

  "GET /users/:id - not authorized" in {
    expectNotAuthorized {
      anonClient.users.getById(UUID.randomUUID.toString)
    }
  }

  "GET /users by id" in {
    await(
      identifiedClient().users.get(id = Some(user1.id))
    ).map(_.id) must be(
      Seq(user1.id)
    )

    await(
      identifiedClient().users.get(id = Some(UUID.randomUUID.toString))
    ).map(_.id) must be(
      Nil
    )
  }

  "GET /users by email" in {
    await(
      identifiedClient().users.get(email = user1.email)
    ).map(_.email) must be(
      Seq(user1.email)
    )

    await(
      identifiedClient().users.get(email = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /users/:id" in {
    await(identifiedClient().users.getById(user1.id)).id must be(user1.id)
    await(identifiedClient().users.getById(user2.id)).id must be(user2.id)

    expectNotFound {
      identifiedClient().users.getById(UUID.randomUUID.toString)
    }
  }

  "POST /users w/out name" in {
    val email = createTestEmail()
    val user = await(anonClient.users.post(UserForm(email = Some(email))))
    user.email must be(Some(email))
    user.name.first must be(None)
    user.name.last must be(None)
  }

  "POST /users w/ name" in {
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
    user.email must be(Some(email))
    user.name.first must be(Some("Michael"))
    user.name.last must be(Some("Bryzek"))
  }

  "POST /users validates duplicate email" in {
    expectErrors(
      anonClient.users.post(UserForm(email = Some(user1.email.get)))
    ).genericError.messages must be(
      Seq("Email is already registered")
    )
  }

  "POST /users validates empty email" in {
    expectErrors(
      anonClient.users.post(UserForm(email = Some("   ")))
    ).genericError.messages must be(
      Seq("Email address cannot be empty")
    )
  }

  "POST /users validates email address format" in {
    expectErrors(
      anonClient.users.post(UserForm(email = Some("mbfoo.com")))
    ).genericError.messages must be(
      Seq("Please enter a valid email address")
    )
  }

}
