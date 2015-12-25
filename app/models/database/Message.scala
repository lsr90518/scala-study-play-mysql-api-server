package models.database

import play.api.libs.json._

case class Message (id: Option[Long], text: String)

object Message {
  implicit val messageWrites = Json.writes[Message]
  implicit val messageReads = Json.reads[Message]
}
