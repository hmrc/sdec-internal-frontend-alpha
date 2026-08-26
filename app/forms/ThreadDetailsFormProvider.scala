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
import play.api.data.validation.ValidationError
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.LocalDate
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

  private val responseDateRequiredWhenYes: Constraint[ThreadDetails] =
    Constraint {
      case ThreadDetails(_, true, None) =>
        Invalid(
          ValidationError("threadDetails.responseDate.error.requiredWhenYes")
        )

      case _ =>
        Valid
    }

  private val responseDateMustBeInFuture: Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(LocalDate.now()) =>
        Valid

      case _ =>
        Invalid("threadDetails.responseDate.error.future")
    }

  def apply()(implicit messages: Messages): Form[ThreadDetails] = Form(
    mapping(
      "message" -> text("threadDetails.error.message.required")
        .verifying(
          maxLengthWithOverage(
            messageMaxLength,
            "threadDetails.error.message.length"
          )
        ),
      "responseRequired" ->
        boolean(
          requiredKey = "threadDetails.responseRequired.error.required"
        ),
      "responseDate" ->
        optional(
          localDate(
            invalidKey = "threadDetails.responseDate.error.invalid",
            allRequiredKey = "threadDetails.responseDate.error.all.required",
            twoRequiredKey = "threadDetails.responseDate.error.two.required",
            requiredKey = "threadDetails.responseDate.error.required"
          ).verifying(responseDateMustBeInFuture)
        )
    )(
      ThreadDetails.apply
    )(threadDetails =>
      Some(
        (
          threadDetails.message,
          threadDetails.responseRequired,
          threadDetails.responseDate
        )
      )
    ).verifying(responseDateRequiredWhenYes)
  )
}
