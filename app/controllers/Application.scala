package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._;

import javax.inject.Inject

import models.database.Message
import models.MessageDAO

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (dao: MessageDAO) extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def message = Action.async {
    dao.all().map(messages => Ok(Json.toJson(Json.toJson(messages))))
  }
}
