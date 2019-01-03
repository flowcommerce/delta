package db

import java.util.UUID

import io.flow.delta.v0.models.Publication
import io.flow.test.utils.FlowPlaySpec

class SubscriptionsDaoSpec extends FlowPlaySpec with Helpers {

  "upsert" in {
    val form = createSubscriptionForm()
    val subscription1 = subscriptionsDao.upsert(systemUser, form)

    val subscription2 = subscriptionsDao.upsert(systemUser, form)
    subscription1.id must be(subscription2.id)

    val subscription3 = createSubscription()

    subscription2.id must not be(subscription3.id)
  }

  "findById" in {
    val subscription = createSubscription()
    subscriptionsDao.findById(subscription.id).map(_.id) must be(
      Some(subscription.id)
    )

    subscriptionsDao.findById(UUID.randomUUID.toString) must be(None)
  }

  "findByUserIdAndPublication" in {
    val subscription = createSubscription()
    subscriptionsDao.findByUserIdAndPublication(subscription.user.id, subscription.publication).map(_.id) must be(
      Some(subscription.id)
    )

    subscriptionsDao.findByUserIdAndPublication(UUID.randomUUID.toString, subscription.publication).map(_.id) must be(None)
    subscriptionsDao.findByUserIdAndPublication(subscription.user.id, Publication.UNDEFINED("other")).map(_.id) must be(None)
  }

  "findAll by ids" in {
    val subscription1 = createSubscription()
    val subscription2 = createSubscription()

    subscriptionsDao.findAll(ids = Some(Seq(subscription1.id, subscription2.id))).map(_.id) must be(
      Seq(subscription1.id, subscription2.id)
    )

    subscriptionsDao.findAll(ids = Some(Nil)) must be(Nil)
    subscriptionsDao.findAll(ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    subscriptionsDao.findAll(ids = Some(Seq(subscription1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(subscription1.id))
  }

  "findAll by identifier" in {
    val user = createUserReference()
    val subscription = subscriptionsDao.upsert(systemUser, createSubscriptionForm(user = user))
    val identifier = userIdentifiersDao.latestForUser(systemUser, user).value

    subscriptionsDao.findAll(identifier = Some(identifier)).map(_.id) must be(Seq(subscription.id))
    subscriptionsDao.findAll(identifier = Some(createTestKey())) must be(Nil)
  }

}
