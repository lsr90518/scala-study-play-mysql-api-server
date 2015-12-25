package models

import javax.inject.Singleton
import javax.inject.Inject

import scala.concurrent.Future

import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import models.database.Message

import play.api.libs.json._

@Singleton
class MessageDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig.driver.api._

  private class MessageTable(tag: Tag) extends Table[Message](tag, "messages") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def text= column[String]("text")

    def * = (id.?, text) <> ((Message.apply _).tupled, Message.unapply)
  }

  private val messages = TableQuery[MessageTable]

  def all(): Future[List[Message]] = dbConfig.db.run(messages.result).map(_.toList)

  def create(message: Message): Future[Int] = {
    val n = message.copy()
    dbConfig.db.run(messages += n)
  }
}
