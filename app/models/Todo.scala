package models

import anorm._
import anorm.SqlParser._

import controllers.routes

import global.Address

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.db.DB
import play.api.Play.current

trait Todo {
  val title: String
  val completed: Boolean
  val order: Option[Long]
}

case class SavedTodo(title: String, completed: Boolean, order: Option[Long], id: Long) extends Todo {
  def url: String = routes.TodoItem.get(id).absoluteURL(Address.secure, Address.hostname)

  def update() = DB.withConnection{ implicit c =>
    val rows = SQL"""
    UPDATE Todo
      SET title=$title, completed=$completed, "order"=$order
      WHERE id=$id
    """.executeUpdate()
    this.ensuring(rows == 1, s"updated too many rows when updating $this")
    this
  }
}


case class WireTodo(title: String, completed: Boolean, order: Option[Long]) extends Todo

object Todo {
  val titlePath = JsPath \ "title"
  val completedPath = JsPath \ "completed"
  val urlPath = JsPath \ "url"
  val orderPath = JsPath \ "order"

  implicit val todoReads: Reads[WireTodo] = (
    titlePath.read[String] and
    completedPath.readNullable[Boolean].map(_.getOrElse(false)) and
    orderPath.readNullable[Long]
  )(WireTodo.apply _)

  implicit val todoWrites: Writes[SavedTodo] = new Writes[SavedTodo] {
    def writes(t: SavedTodo) = Json.obj(
      "title" -> t.title,
      "completed" -> t.completed,
      "order" -> t.order,
      "url" -> t.url
    )
  }

  val parser: RowParser[SavedTodo] = (str("title") ~ bool("completed") ~ long("order").? ~ long("id"))
    .map(flatten)
    .map{ case (title, completed, order, id) => SavedTodo(title, completed, order, id) }

  def get(id: Long): SavedTodo = DB.withConnection { implicit c =>
    SQL"""
      SELECT id, title, completed, "order"
      FROM Todo
      WHERE id=$id
    """.as(parser.single)
  }

  def save(t: WireTodo) = DB.withConnection { implicit c =>
    val id: Option[Long] = SQL"""
      INSERT INTO Todo
        (title, completed, "order")
      VALUES
        (${t.title}, ${t.completed}, ${t.order})
    """.executeInsert()
    SavedTodo(t.title, t.completed, t.order, id.get)
  }

  def all(): Seq[SavedTodo] = DB.withConnection { implicit c =>
    SQL"""
      SELECT id, title, completed, "order"
      FROM Todo
    """.as(parser.*)
  }

  def clear() = DB.withConnection{ implicit c =>
    SQL"""
      TRUNCATE TABLE Todo
    """.execute()
  }

  def delete(id: Long) = DB.withConnection { implicit c =>
    SQL"""
      DELETE FROM Todo
      WHERE id=$id
    """.execute()
  }
}
