package models

import ai.x.play.json.Jsonx
import anorm.{Macro, RowParser}
import play.api.libs.json.OFormat

case class SongLyrics(id: Long, lyricsText: Option[String])

object SongLyrics {
  val parser: RowParser[SongLyrics] = Macro.namedParser[SongLyrics]
  implicit val format: OFormat[SongLyrics] = Jsonx.formatCaseClass[SongLyrics]
}