package io.flow.delta.lib

import org.joda.time.{DateTime, Period}

object Text {

  private[this] val Ellipsis = "..."

  def pluralize(num: Long, singular: String, plural: String) = {
    num match {
      case 1 => s"1 $singular"
      case _ => s"$num $plural"
    }
  }

  /**
   * if value is longer than maxLength characters, it wil be truncated
   * to <= (maxLength-Ellipsis.length) characters and an ellipsis
   * added. We try to truncate on a space to avoid breaking a word in
   * pieces.
   */
  // From https://github.com/mbryzek/apidoc/blob/6186612993a0c913cfd0b7a36417bda45281825e/lib/src/main/scala/Text.scala
  def truncate(value: String, maxLength: Int = 100): String = {
    require(maxLength >= 10, "maxLength must be >= 10")

    if (value.length <= maxLength) {
      value
    } else {
      val pieces = value.split(" ")
      var i = pieces.length
      while (i > 0) {
        val sentence = pieces.slice(0, i).mkString(" ")
        if (sentence.length <= (maxLength-Ellipsis.length)) {
          return sentence + Ellipsis
        }
        i -= 1
      }

      val letters = value.split("")
      letters.slice(0, letters.length-4).mkString("") + Ellipsis
    }
  }

  /**
   * Returns a human label describing the approximate difference since
   * a particular time (e.g. 2 minutes ago).
   */
  def since(date: DateTime, base: DateTime = new DateTime()): String = {
    val period = new Period(date, base)

    val label = if (period.getYears > 0) {
      Text.pluralize(period.getYears.toLong, "year", "years")

    } else if (period.getMonths > 0) {
      Text.pluralize(period.getMonths.toLong, "month", "months")

    } else if (period.getWeeks > 0) {
      Text.pluralize(period.getWeeks.toLong, "week", "weeks")

    } else if (period.getDays > 0) {
      Text.pluralize(period.getDays.toLong, "day", "days")

    } else if (period.getHours > 0) {
      Text.pluralize(period.getHours.toLong, "hour", "hours")

    } else if (period.getMinutes > 0) {
      Text.pluralize(period.getMinutes.toLong, "minute", "minutes")

    } else {
      "seconds"
    }

    label + " ago"
  }

}
