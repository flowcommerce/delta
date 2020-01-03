package io.flow.delta.lib

case class HypermediaLink(url: String, rel: HypermediaLinkRel)

sealed trait HypermediaLinkRel
object HypermediaLinkRel {
  case object Last extends HypermediaLinkRel
  case object First extends HypermediaLinkRel
  case object Next extends HypermediaLinkRel
  case object Previous extends HypermediaLinkRel
  case class Other(name: String) extends HypermediaLinkRel

  def apply(value: String): HypermediaLinkRel = {
    value.trim.toLowerCase match {
      case "next" => Next
      case "last" => Last
      case "first" => First
      case "previous" | "prev" => Previous
      case _ => Other(value)
    }
  }
}

object HypermediaLink {
  def parse(value: String): Either[Seq[String], HypermediaLink] = {
    value.trim.split(";").map(_.trim).filter(_.nonEmpty).toList match {
      case url :: rel :: Nil => {
        val validatedUrl = validateUrl(url)
        val validatedRel = validateRel(rel)
        Seq(validatedUrl, validatedRel).flatMap(_.left.getOrElse(Nil)).toList match {
          case Nil => {
            Right(
              HypermediaLink(
                url = validatedUrl.right.get,
                rel = validatedRel.right.get,
              )
            )
          }
          case errors => {
            Left(errors)
          }
        }
      }
      case _ => {
        Left(Seq(s"Cannot parse link value '$value': expected a single semi colon"))
      }
    }
  }

  def validateUrl(url: String): Either[Seq[String], String] = {
    if (url.startsWith("<") && url.endsWith(">")) {
      Right(url.drop(1).dropRight(1))
    } else {
      Left(Seq(s"Cannot parse link url '$url': Must start with '<' and end with '>'"))
    }
  }

  def validateRel(rel: String): Either[Seq[String], HypermediaLinkRel] = {
    val trimmed = rel.replaceAll(" ", "").replaceAll("\"", "").trim
    if (trimmed.startsWith("rel=")) {
      Right(HypermediaLinkRel(trimmed.drop(4)))
    } else {
      Left(Seq(s"Cannot parse link rel '$rel': Must start with rel="))
    }
  }
}
