package db

import io.flow.delta.v0.models.Publication
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class SubscriptionsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsert" in {
    val form = createSubscriptionForm()
    val subscription1 = SubscriptionsDao.upsert(systemUser, form)

    val subscription2 = SubscriptionsDao.upsert(systemUser, form)
    subscription1.id must be(subscription2.id)

    val newSubscription = UUID.randomUUID.toString
    val subscription3 = createSubscription()

    subscription2.id must not be(subscription3.id)
  }

  "findById" in {
    val subscription = createSubscription()
    SubscriptionsDao.findById(subscription.id).map(_.id) must be(
      Some(subscription.id)
    )

    SubscriptionsDao.findById(UUID.randomUUID.toString) must be(None)
  }

  "findByUserIdAndPublication" in {
    val subscription = createSubscription()
    SubscriptionsDao.findByUserIdAndPublication(subscription.user.id, subscription.publication).map(_.id) must be(
      Some(subscription.id)
    )

    SubscriptionsDao.findByUserIdAndPublication(UUID.randomUUID.toString, subscription.publication).map(_.id) must be(None)
    SubscriptionsDao.findByUserIdAndPublication(subscription.user.id, Publication.UNDEFINED("other")).map(_.id) must be(None)
  }

  "findAll by ids" in {
    val subscription1 = createSubscription()
    val subscription2 = createSubscription()

    SubscriptionsDao.findAll(ids = Some(Seq(subscription1.id, subscription2.id))).map(_.id) must be(
      Seq(subscription1.id, subscription2.id)
    )

    SubscriptionsDao.findAll(ids = Some(Nil)) must be(Nil)
    SubscriptionsDao.findAll(ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    SubscriptionsDao.findAll(ids = Some(Seq(subscription1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(subscription1.id))
  }

  "findAll by identifier" in {
    val user = createUser()
    val subscription = db.SubscriptionsDao.upsert(systemUser, createSubscriptionForm(user = user))
    val identifier = UserIdentifiersDao.latestForUser(systemUser, user).value

    SubscriptionsDao.findAll(identifier = Some(identifier)).map(_.id) must be(Seq(subscription.id))
    SubscriptionsDao.findAll(identifier = Some(createTestKey())) must be(Nil)
  }

}
