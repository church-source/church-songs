package controllers

import javax.inject._
import play.api.mvc._
import anorm._
import play.api.db.Database
import play.api.libs.json._
import anorm.SqlParser._
import models.{Artist, Song, SongLyrics}
import play.api.{Configuration, Logger}
import security.ViewSongsAuthAction
import security.AddSongsAuthAction
import security.EditSongsAuthAction

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SongController @Inject()(
                                val controllerComponents: ControllerComponents,
                                db: Database,
                                viewSongsAuthAction: ViewSongsAuthAction,
                                editSongsAuthAction: EditSongsAuthAction,
                                addSongsAuthAction: AddSongsAuthAction,
                                config: Configuration) extends BaseController {

  val logger = Logger("application")
  val sheetTypes = List("guitar", "lead", "piano", "lyrics");
  private def sheetDir = config.get[String]("sheet.dir")

  def index(id: Long): Action[AnyContent] = viewSongsAuthAction {

    db.withConnection { implicit c =>
          SQL"""
             SELECT songs.id,
                    songs.code,
                    songs.name,
                    songs.secondary_name as secondaryName,
                    songs.song_key as songKey,
                    artists.id as artistId,
                    artists.name as artistName,
                    songs.style,
                    songs.tempo,
                    songs.ccli_number as ccliNumber,
                    songs.video_link as videoLink,
                    songs.piano_sheet as pianoSheet,
                    songs.lead_sheet as leadSheet,
                    songs.guitar_sheet as guitarSheet,
                    songs.lyrics_sheet as lyricsSheet
             from songs left join artists ON (songs.artist = artists.id)
             where songs.id = $id
             """.as(Song.parser.singleOpt) match {
            case Some(song) => Ok(Json.toJson(song))
            case None => NotFound(Json.obj("error" -> "Not Found"))
          }
    }
  }

  def getSheet(code: String, sheetType: String): Action[AnyContent] = viewSongsAuthAction {
    db.withConnection { implicit c =>
      val st = sheetType.toLowerCase;
      if(!sheetTypes.contains(st)) {
        BadRequest(Json.obj("error" -> "Type not supported"))
      }
      SQL"""
             SELECT 1
             from songs
             where songs.code = $code
             """.as(scalar[Long].singleOpt) match {
        case Some(exists) => {
          var dir = sheetDir;
          if(!dir.endsWith("/")) {
            dir = dir + "/";
          }
          Ok.sendFile(new java.io.File(dir + code + "_" + sheetType + ".pdf"))
        }
        case None => NotFound(Json.obj("error" -> "No song with that code found"))
      }
    }
  }

  def getSheetFromFileName(fileName: String): Action[AnyContent] = Action {
    db.withConnection { implicit c =>
      try {
        if (fileName == null || fileName.isEmpty || fileName.split("_").length != 2) {
          throw new Exception("Invalid file name provided")
        }
        val code = fileName.split("_")(0)
        var st = fileName.split("_")(1).toLowerCase
        st = st.replace(".pdf", "")
        if (!sheetTypes.contains(st)) {
          throw new Exception("Invalid Sheet Type provided")
          //return BadRequest(Json.obj("error" -> "Type not supported"))
        }
        SQL"""
             SELECT 1
             from songs
             where songs.code = $code
             """.as(scalar[Long].singleOpt) match {
          case Some(exists) => {
            var dir = sheetDir;
            if (!dir.endsWith("/")) {
              dir = dir + "/";
            }
            try {
              Ok.sendFile(new java.io.File(dir + code + "_" + st + ".pdf"))
            } catch {
              case e: Throwable => throw new Exception("Something went wrong trying to load the sheet file.");
            }
          }
          case None => NotFound(Json.obj("error" -> "No song with that code found"))
        }
      } catch {
        case e: Throwable => BadRequest(Json.obj("error" -> e.getMessage))

      }
    }
  }

  def getLyrics(id: String): Action[AnyContent] = viewSongsAuthAction {
    db.withConnection { implicit c =>
      val lyrics = SQL"""
             SELECT songs.lyrics_text
             from songs
             where songs.id = $id
             """.as(scalar[String].singleOpt)

      lyrics match {
        case Some(lyrics) => Ok(lyrics)
        case None => NotFound(Json.obj("error" -> "Not Found"))
      }
    }
  }

  def findByCode(code: String): Action[AnyContent] = viewSongsAuthAction {
    db.withConnection { implicit c =>
      SQL"""
             SELECT songs.id,
                    songs.code,
                    songs.name,
                    songs.secondary_name as secondaryName,
                    songs.song_key as songKey,
                    artists.id as artistId,
                    artists.name as artistName,
                    songs.style,
                    songs.tempo,
                    songs.ccli_number as ccliNumber,
                    songs.video_link as videoLink,
                    songs.piano_sheet as pianoSheet,
                    songs.lead_sheet as leadSheet,
                    songs.guitar_sheet as guitarSheet,
                    songs.lyrics_sheet as lyricsSheet
             from songs left join artists ON (songs.artist = artists.id)
             where songs.code = $code
             """.as(Song.parser.singleOpt) match {
        case Some(song) => Ok(Json.toJson(song))
        case None => NotFound(Json.obj("error" -> "Not Found"))
      }
    }
  }

  def list(search: Option[String], artistId: Option[Long], includeTextSearch: Boolean, offset: Int, limit: Int): Action[AnyContent] = viewSongsAuthAction {

    db.withConnection { implicit c =>
      val songs =
        SQL"""
             SELECT songs.id,
                    songs.code,
                    songs.name,
                    songs.secondary_name as secondaryName,
                    songs.song_key as songKey,
                    artists.id as artistId,
                    artists.name as artistName,
                    songs.style,
                    songs.tempo,
                    songs.ccli_number as ccliNumber,
                    songs.video_link as videoLink,
                    songs.piano_sheet as pianoSheet,
                    songs.lead_sheet as leadSheet,
                    songs.guitar_sheet as guitarSheet,
                    songs.lyrics_sheet as lyricsSheet
             from songs left join artists ON (songs.artist = artists.id) where
             (($search is null OR (match (songs.name, secondary_name) AGAINST ($search))) OR
              (($includeTextSearch is true AND $search is null) OR ($includeTextSearch is true AND (match (songs.lyrics_text) AGAINST ($search))))) AND
             ($artistId is null OR (artists.id = $artistId))
             order by left(code,1), length(code), code
             limit $limit offset $offset
             """.as(Song.parser.*)
      Ok(Json.toJson(songs))
    }
  }
//              (($search is null OR (match () AGAINST ($search))) OR
  def insert(): Action[JsValue] = addSongsAuthAction(parse.json) { req =>
      Json.fromJson[Song](req.body) match {
        case JsSuccess(song, _) =>

          val id: Option[Long] = db.withConnection { implicit c =>
            val sql = SQL"""
               insert into songs (code,name,secondary_name,song_key,artist,style,tempo,ccli_number,video_link,piano_sheet,lead_sheet,guitar_sheet,lyrics_sheet)
               values (${song.code}, ${song.name}, ${song.secondaryName}, ${song.songKey}, ${song.artist.artistId}, ${song.style}, ${song.tempo}, ${song.ccliNumber}, ${song.videoLink}, ${song.pianoSheet}, ${song.leadSheet}, ${song.guitarSheet}, ${song.lyricsSheet})
               """
            val id = sql.executeInsert()
            id
          }
          logger.info(s"Inserted song $id")
          Created(Json.obj("id" -> id))

        case e: JsError => {
          println(e)
          BadRequest(Json.obj("err" -> "Invalid Song"))
        }
      }
  }

  def update(id: Long): Action[JsValue] = editSongsAuthAction(parse.json) { req =>
    Json.fromJson[Song](req.body) match {
      case JsSuccess(song, _) =>
        db.withConnection { implicit c =>
          val updateRes = SQL"""
                 update songs set
                   code = ${song.code},
                   name = ${song.name},
                   secondary_name = ${song.secondaryName},
                   song_key = ${song.songKey},
                   artist = ${song.artist.artistId},
                   style = ${song.style},
                   tempo = ${song.tempo},
                   ccli_number = ${song.ccliNumber},
                   video_link = ${song.videoLink},
                   piano_sheet = ${song.pianoSheet},
                   lead_sheet = ${song.leadSheet},
                   guitar_sheet = ${song.guitarSheet},
                   lyrics_sheet = ${song.lyricsSheet}
                 where songs.id = $id
                 """.executeUpdate()
          logger.info(s"Updated song $id")
          Ok(Json.obj("updated" -> updateRes))
        }
      case _ => BadRequest(Json.obj("err" -> "Invalid Song"))
    }
  }

  def updateLyrics(id: Long): Action[JsValue] = editSongsAuthAction(parse.json) { req =>
    Json.fromJson[SongLyrics](req.body) match {
      case JsSuccess(song, _) =>
        db.withConnection { implicit c =>
          val updateRes = SQL"""
                 update songs set
                   lyrics_text = ${song.lyricsText}
                 where songs.id = $id
                 """.executeUpdate()
          logger.info(s"Updated song $id")
          Ok(Json.obj("updated" -> updateRes))
        }
      case _ => BadRequest(Json.obj("err" -> "Invalid Song"))
    }
  }
}
