package io.flow.delta.api.lib

import io.flow.common.v0.models.Name
import io.flow.play.util.{DefaultConfig, IdGenerator}
import java.nio.file.{Files, Path, Paths}
import java.nio.charset.StandardCharsets

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.sendgrid._

class Email @javax.inject.Inject() (
  config: DefaultConfig
) {

  private[this] val SubjectPrefix = config.requiredString("mail.subject.prefix")

  def subjectWithPrefix(subject: String): String = {
    SubjectPrefix + " " + subject
  }

  val fromEmail = config.requiredString("mail.default.from.email")
  val fromName = Name(
    Some(config.requiredString("mail.default.from.name.first")),
    Some(config.requiredString("mail.default.from.name.last"))
  )

  val localDeliveryDir = config.optionalString("mail.local.delivery.dir").map(Paths.get(_))

  // Initialize sendgrid on startup to verify that all of our settings
  // are here. If using localDeliveryDir, set password to a test
  // string.
  private[this] val sendgrid = {
    localDeliveryDir match {
      case None => new SendGrid(config.requiredString("sendgrid.api.key"))
      case Some(_) => new SendGrid("development")
    }
  }

  def sendHtml(
    recipient: Recipient,
    subject: String,
    body: String
  ) {
    val prefixedSubject = subjectWithPrefix(subject)

    val from = new com.sendgrid.Email(fromEmail)
    val to = recipient.fullName() match {
      case Some(fn) => new com.sendgrid.Email(recipient.email, fn)
      case None => new com.sendgrid.Email(recipient.email)
    }
    val content = new Content("text/html", body)

    val mail = new com.sendgrid.Mail(from, prefixedSubject, to, content)

    localDeliveryDir match {
      case Some(dir) => {
        localDelivery(dir, recipient, prefixedSubject, body)
      }

      case None => {
        val request = new Request()
        request.setMethod(Method.POST)
        request.setEndpoint("mail/send")
        request.setBody(mail.build())
        val response = sendgrid.api(request)
        assert(
          response.getStatusCode() == 202,
          s"Error sending email. Expected statusCode[202] but got[${response.getStatusCode()}]"
        )
      }
    }
  }

  private[this] def fullName(name: Name): Option[String] = {
    Seq(name.first, name.last).flatten.toList match {
      case Nil => None
      case names => Some(names.mkString(" "))
    }
  }

  private[this] def localDelivery(dir: Path, to: Recipient, subject: String, body: String): String = {
    val timestamp = ISODateTimeFormat.dateTimeNoMillis.print(new DateTime())

    Files.createDirectories(dir)
    val target = Paths.get(dir.toString, timestamp + "-" + IdGenerator("eml").randomId() + ".html")
    val name = fullName(to.name) match {
      case None => to.email
      case Some(name) => s""""$name" <${to.email}">"""
    }

    val bytes = s"""<p>
To: $name<br/>
Subject: $subject
</p>
<hr size="1"/>

$body
""".getBytes(StandardCharsets.UTF_8)
    Files.write(target, bytes)

    println(s"email delivered locally to $target")
    s"local-delivery-to-$target"
  }

}
