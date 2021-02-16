package models

import play.api.libs.json.{JsValue, OFormat}
import anorm._
import ai.x.play.json.Jsonx
import play.api.Logger

final case class Artist(
                   artistId: Option[Long] = None,
                   artistName: Option[String] = None
                 )

object Artist {
  implicit val format: OFormat[Artist] = Jsonx.formatCaseClass[Artist]

  val parser: RowParser[Artist] = Macro.namedParser[Artist]
}