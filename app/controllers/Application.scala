package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._;

class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def message = Action {
    val json = Json.obj("message" -> "Hello!");

    Ok(Json.toJson(json));
  }
}
