package db

import io.flow.delta.v0.models.EventType
import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class EventsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "create" in {
    val project = createProject()
    val id = EventsDao.create(systemUser, project.id, EventType.Info, "test", ex = None)
    val event = EventsDao.findById(id).getOrElse {
      sys.error("Failed to create event")
    }
    event.`type` must be(EventType.Info)
    event.summary must be("test")
    event.error must be(None)
  }

  "findById" in {
    val event = createEvent()
    EventsDao.findById(event.id).map(_.id) must be(
      Some(event.id)
    )

    EventsDao.findById(UUID.randomUUID.toString) must be(None)
  }

  "findAll by ids" in {
    val event1 = createEvent()
    val event2 = createEvent()

    EventsDao.findAll(ids = Some(Seq(event1.id, event2.id))).map(_.id).sorted must be(
      Seq(event1.id, event2.id).sorted
    )

    EventsDao.findAll(ids = Some(Nil)) must be(Nil)
    EventsDao.findAll(ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    EventsDao.findAll(ids = Some(Seq(event1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(event1.id))
  }

  "findAll by projectId" in {
    val project1 = createProject()
    val project2 = createProject()

    val event1 = createEvent(project1)
    val event2 = createEvent(project2)
    val ids = Seq(event1.id, event2.id)

    EventsDao.findAll(ids = Some(ids), projectId = Some(project1.id)).map(_.id).sorted must be(
      Seq(event1.id)
    )

    EventsDao.findAll(ids = Some(ids), projectId = Some(project2.id)).map(_.id).sorted must be(
      Seq(event2.id)
    )

    EventsDao.findAll(projectId = Some(createTestKey())) must be(Nil)
  }

  "findAll by numberMinutesSinceCreation" in {
    val event = createEvent()

    EventsDao.findAll(ids = Some(Seq(event.id)), numberMinutesSinceCreation = Some(10)).map(_.id) must be(Seq(event.id))

    DirectDbAccess.setCreatedAt("events", event.id, -15)
    EventsDao.findAll(ids = Some(Seq(event.id)), numberMinutesSinceCreation = Some(10)) must be(Nil)
  }

}
