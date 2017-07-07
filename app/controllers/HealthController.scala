package controllers

import play.api.mvc.{ Action, Controller }

import org.joda.time.DateTime
import play.api.mvc.{ Controller, _ }

/**
 * ...............
 */
class HealthController extends Controller {
  private[this] val startTime = System.currentTimeMillis()

  def status = Action {
    val uptimeInMillis = uptime()
    Ok(s"{Status: Ok, Uptime: ${uptimeInMillis}ms, Date and Time: " + new DateTime(startTime) + "}").as(JSON)
  }

  private def uptime(): Long = {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }

  // stats
  def health = Action {
    Ok("application health and status!!")
  }
}
