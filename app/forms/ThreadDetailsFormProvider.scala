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

package forms

import forms.mappings.Mappings
import models.ThreadDetails
import play.api.data.Form
import play.api.data.Forms.*
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class ThreadDetailsFormProvider @Inject() extends Mappings {

  private val messageMaxLength = 1000

  private def maxLengthWithOverage(
    maximum:  Int,
    errorKey: String
  ): Constraint[String] =
    Constraint {
      case str if str.length <= maximum =>
        Valid
      case str =>
        Invalid(errorKey, str.length - maximum)
    }

  def apply(): Form[ThreadDetails] = Form(
    mapping(
      "message" -> text("threadDetails.error.message.required")
        .verifying(
          maxLengthWithOverage(
            messageMaxLength,
            "threadDetails.error.message.length"
          )
        )
    )(ThreadDetails.apply)(td => Some(td.message))
  )
}
