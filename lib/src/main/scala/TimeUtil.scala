import java.time.{Instant, ZonedDateTime}

import org.joda.time.DateTime

package object TimeUtil {
  def toZonedDateTime(originDateTime: DateTime): ZonedDateTime = {
    ZonedDateTime.ofInstant(
      Instant.ofEpochMilli(originDateTime.getMillis),
      java.time.ZoneId.of(originDateTime.getZone.getID)
    )
  }
}