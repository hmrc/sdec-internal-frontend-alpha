/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.libs.json.*
import play.api.mvc.PathBindable

import scala.util.matching.Regex

final case class ThreadReference private (value: String)

object ThreadReference {
  private val ThreadReferenceRegex: Regex = """^[A-Z0-9]{12}$""".r

  def from(value: String): Either[ThreadReferenceError, ThreadReference] =
    value match {
      case ""                                       => Left(ThreadReferenceError.Empty)
      case str if str.length != 12                  => Left(ThreadReferenceError.InvalidLength)
      case str if ThreadReferenceRegex.matches(str) => Right(ThreadReference(str))
      case str                                      => Left(ThreadReferenceError.InvalidFormat(str))
    }

  given PathBindable[ThreadReference] with {
    def bind(key: String, value: String): Either[String, ThreadReference] =
      ThreadReference.from(value).left.map(_ => "Invalid Thread Reference")

    def unbind(key: String, threadReference: ThreadReference): String = threadReference.value
  }

  given format: OFormat[ThreadReference] = OFormat(
    Reads[ThreadReference] { json =>
      (json \ "value").validate[String].flatMap { value =>
        ThreadReference.from(value).fold(
          error => JsError(s"Invalid thread reference: $error"),
          reference => JsSuccess(reference)
        )
      }
    },
    OWrites[ThreadReference] { reference =>
      Json.obj("value" -> reference.value)
    }
  )

}
