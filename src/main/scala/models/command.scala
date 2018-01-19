package models

import io.circe.Decoder
import io.circe.generic.semiauto._

case class Command(action: String, domain: String, target: String)

object Command {
  val decoder: Decoder[Command] = deriveDecoder[Command]
}