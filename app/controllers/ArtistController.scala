package controllers

import javax.inject._
import play.api.mvc._
import anorm._
import play.api.db.Database
import play.api.libs.json._
import anorm.SqlParser._
import models.Artist
import play.api.Logger


@Singleton
class ArtistController @Inject()(val controllerComponents: ControllerComponents, db: Database) extends BaseController {

  val logger = Logger("application")

  def index(id: Long): Action[AnyContent] = Action {

    db.withConnection { implicit c =>
          SQL"""
             SELECT artists.id as artistId,
                    artists.name as artistName
             from artists
             where artists.id = $id
             """.as(Artist.parser.singleOpt) match {
            case Some(artist) => Ok(Json.toJson(artist))
            case None => NotFound(Json.obj("error" -> "Not Found"))
          }
    }
  }

  def list(offset: Int, limit: Int): Action[AnyContent] = Action {

    db.withConnection { implicit c =>
      val artists =
        SQL"""
             SELECT artists.id as artistId,
                    artists.name as artistName
             from artists
             order by artists.name desc
             limit $limit offset $offset
             """.as(Artist.parser.*)
      Ok(Json.toJson(artists))
    }
  }

  def insert(): Action[JsValue] = Action(parse.json) { req =>
      Json.fromJson[Artist](req.body) match {
        case JsSuccess(artist, _) =>

          val id: Option[Long] = db.withConnection { implicit c =>
            val sql = SQL"""
               insert into artists (name)
               values (${artist.artistName})
               """
            val id = sql.executeInsert()
            id
          }
          logger.info(s"Inserted artist $id")
          Created(Json.obj("id" -> id))

        case e: JsError => {
          println(e)
          BadRequest(Json.obj("err" -> "Invalid Artust"))
        }
      }
  }

  def update(id: Long): Action[JsValue] = Action(parse.json) { req =>
    Json.fromJson[Artist](req.body) match {
      case JsSuccess(artist, _) =>
        db.withConnection { implicit c =>
          val updateRes = SQL"""
                 update artists set
                   name = ${artist.artistName}
                 where artists.id = $id
                 """.executeUpdate()
          logger.info(s"Updated artist $id")
          Ok(Json.obj("updated" -> updateRes))
        }
      case _ => BadRequest(Json.obj("err" -> "Invalid Artist"))
    }
  }
}
