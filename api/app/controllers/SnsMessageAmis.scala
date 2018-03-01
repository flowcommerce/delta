package controllers

import java.io.{ByteArrayInputStream, InputStream}
import javax.inject.Inject

import com.amazonaws.SdkClientException
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.message._
import com.amazonaws.services.sns.model.{ConfirmSubscriptionRequest, ConfirmSubscriptionResult}
import com.amazonaws.services.sns.{AmazonSNSAsyncClient, AmazonSNSClientBuilder}
import io.flow.delta.aws.{Configuration, Credentials}
import io.flow.delta.v0.models.SnsMessageAmi
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SnsMessageAmis @Inject()(
  val controllerComponents: ControllerComponents
) extends BaseController {

  private val logger = Logger(getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  def post() = Action.async(parse.tolerantText) { request =>

    logger.info(s"SNS handler received: ${request.body}")

    val manager = new SnsMessageManager(Regions.US_EAST_1.getName)

    // the SDK will verify that the message is signed by AWS
    manager.handleMessage(new ByteArrayInputStream(request.body.getBytes), new SnsMessageHandler {
      override def handle(message: SnsNotification): Unit = {

        Json.parse(message.getMessage).validate[SnsMessageAmi] match {
          case JsSuccess(ami, _) =>

            logger.info(s"Latest ECS-optimized AMI for us-east-1 is ${ami.ECSAmis.Regions.usEast1.ImageId}")

          case JsError(errors) => logger.error(s"Invalid message received: $errors")
        }

      }

      override def handle(message: SnsSubscriptionConfirmation): Unit = {
        logger.info(s"Subscribing to SNS topic ${message.getTopicArn}")
        Try(message.confirmSubscription()) match {
          case Failure(exception: SdkClientException) =>
            logger.error("FlowError: subscribing to SNS topic failed", exception)
          case Success(ConfirmSubscriptionResult) =>
            logger.info("Subscribed")
        }
      }

      override def handle(message: SnsUnsubscribeConfirmation): Unit = {
        logger.info(s"Unsubscribed from ${message.getTopicArn}")
      }

      override def handle(message: SnsUnknownMessage): Unit = {}
    })


    Future {
      Ok
    }

    //      .validate[SnsMessageAmi] match {
    //      case JsSuccess(value, path) =>
    //      case JsError(errors) =>
    //    }
  }

}
