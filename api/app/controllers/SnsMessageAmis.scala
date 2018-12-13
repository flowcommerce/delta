package controllers

import java.io.ByteArrayInputStream

import javax.inject.Inject
import com.amazonaws.SdkClientException
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.message._
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult
import db.generated.{AmiUpdateForm, AmiUpdatesDao}
import io.flow.delta.v0.models.SnsMessageAmi
import io.flow.delta.v0.models.json._
import io.flow.email.v0.models.Email
import io.flow.email.v0.models.json._
import io.flow.email.v0.models.AmiUpdateNotification
import io.flow.event.v2.Queue
import io.flow.log.RollbarLogger
import io.flow.util.Constants
import io.flow.util.IdGenerator
import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class SnsMessageAmis @Inject()(
  val controllerComponents: ControllerComponents,
  dao: AmiUpdatesDao,
  queue: Queue,
  logger: RollbarLogger,
  implicit val ec: ExecutionContext
) extends BaseController {

  private val emails = queue.producer[Email]()

  private val idg = IdGenerator("evt")

  def post() = Action(parse.tolerantText) { request =>
    logger.withKeyValue("request", request.body).info(s"AMI update - got message")

    val manager = new SnsMessageManager(Regions.US_EAST_1.getName)

    // the SDK will verify that the message is signed by AWS
    manager.handleMessage(new ByteArrayInputStream(request.body.getBytes), new SnsMessageHandler {
      override def handle(message: SnsNotification): Unit = {

        Json.parse(message.getMessage).validate[SnsMessageAmi] match {
          case JsSuccess(ami, _) =>

            ami.ECSAmis.foreach { amis =>
              logger.withKeyValue("image_id", amis.Regions.usEast1.ImageId).info(s"Latest ECS-optimized AMI for us-east-1")

              dao.upsertIfChangedById(Constants.SystemUser, AmiUpdateForm(
                amis.Regions.usEast1.ImageId,
                amis.Regions.usEast1.Name
              ))

              emails.publish(AmiUpdateNotification(
                eventId = idg.randomId(),
                timestamp = new DateTime(),
                amiName = amis.Regions.usEast1.Name,
                amiId = amis.Regions.usEast1.ImageId,
              ))
            }

          case JsError(errors) =>
            logger.withKeyValue("request_body", request.body).withKeyValue("message", message.getMessage).withKeyValue("errors", errors.mkString(",")).error(s"FlowError: Invalid message received")
        }

      }

      override def handle(message: SnsSubscriptionConfirmation): Unit = {
        logger.info(s"Subscribing to SNS topic ${message.getTopicArn}")
        Try(message.confirmSubscription()) match {
          case Failure(exception: SdkClientException) =>
            logger.error("FlowAlertError: subscribing to SNS topic failed w/ SdkClientException", exception)
          case Failure(exception: Throwable) =>
            logger.error("FlowAlertError: subscribing to SNS topic failed", exception)
          case Success(_: ConfirmSubscriptionResult) =>
            logger.info("Subscribed")
        }
      }

      override def handle(message: SnsUnsubscribeConfirmation): Unit = {
        logger.withKeyValue("topic", message.getTopicArn).info(s"Unsubscribed from topic")
      }

      override def handle(message: SnsUnknownMessage): Unit = {}
    })

    Ok
  }

}
