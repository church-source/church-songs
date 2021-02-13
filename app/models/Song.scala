package models

import play.api.libs.json.{JsValue, OFormat}
import anorm._
import ai.x.play.json.Jsonx

case class Song(
                   id: Option[Long] = None,
                   code: String,
                   name: String,
                   secondaryName: Option[String],
                   songKey: Option[String],
                   artist: Artist = Artist(),
                   style: Option[String],
                   tempo: Option[String],
                   ccliNumber: Option[String],
                   videoLink: Option[String],
                   pianoSheet: Option[String],
                   leadSheet: Option[String],
                   guitarSheet: Option[String],
                   lyricsSheet: Option[String]
                 )

object Song {
  implicit val artistParser: RowParser[Artist] = Macro.namedParser[Artist]

  val parser: RowParser[Song] = Macro.namedParser[Song]
  implicit val format: OFormat[Song] = Jsonx.formatCaseClass[Song]

}