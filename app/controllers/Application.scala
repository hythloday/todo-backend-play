package controllers

import scala.util.{Failure, Try, Success}

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import models.{Todo, WireTodo}

object TodoCollection extends Controller {

  val log = Logger

  def post = Action(parse.json) { request =>
    request.body.validate[WireTodo].fold(e =>
      BadRequest(JsError.toFlatJson(e)),
    todo => {
      val saved = Todo.save(todo)
      Ok(Json.toJson(saved))
    })
  }

  def get = Action {
    Ok(Json.toJson(Todo.all()))
  }

  def delete = Action(parse.tolerantText) { request =>
    Todo.clear()
    Ok("")
  }
}

object TodoItem extends Controller {

  val log = Logger

  def get(id: Long) = Action {
    Ok(Json.toJson(Todo.get(id)))
  }

  def patch(id: Long) = Action(parse.json) { request =>
    val jsonPatch = request.body.as[JsObject].fields
    val orig = Todo.get(id)
    val patched = jsonPatch.foldLeft(Try(orig)) { (todo, field) =>
      field match {
        case ("completed", JsBoolean(patchCompleted)) => todo.map(t => t.copy(completed = patchCompleted))
        case ("title", JsString(title))               => todo.map(t => t.copy(title=title))
        case ("order", JsNumber(order))               => todo.map(t => t.copy(order=Some(order.toLong)))
        case (key, _) => Failure(new IllegalArgumentException(s"Unexpected key in patch: $key"))
      }
    }
    patched match {
      // Malformed patch
      case Failure(ex) => BadRequest(ex.getMessage)
      // No change
      case Success(`orig`) => Ok(Json.toJson(orig))
      // Change
      case Success(changed) => Ok(Json.toJson(changed.update()))
    }
  }

  def delete(id: Long) = Action(parse.tolerantText) { request =>
    Todo.delete(id)
    Ok("")
  }
}

