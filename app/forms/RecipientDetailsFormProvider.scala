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
import models.RecipientDetails
import play.api.data.Form
import play.api.data.Forms.*
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class RecipientDetailsFormProvider @Inject() extends Mappings {

  private val nameMaxLength   = 100
  private val ninoFormatRegex = "^[A-Z]{2}[0-9]{6}[A-D]$"
  private val ninoValidRegex  =
    "^(?!BG|GB|NK|KN|TN|NT|ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9]{6}[A-D]$"
  private val emailRegex =
    """^[a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$"""
  private val phoneRegex       = """^[0-9 +().-]{9,16}$"""
  private val caseRefMaxLength = 30

  private val caseReferenceRequiredWhenRelated: Constraint[RecipientDetails] =
    Constraint("constraints.caseReferenceNumber") { details =>
      if details.hasRelatedCase && details.caseReferenceNumber.forall(_.trim.isEmpty)
      then {
        Invalid("recipientDetails.error.caseReferenceNumber.required")
      } else {
        Valid
      }
    }

  def apply(): Form[RecipientDetails] = Form(
    mapping(
      "firstName" -> text("recipientDetails.error.firstName.required")
        .verifying(
          maxLength(nameMaxLength, "recipientDetails.error.firstName.length")
        ),
      "lastName" -> text("recipientDetails.error.lastName.required")
        .verifying(
          maxLength(nameMaxLength, "recipientDetails.error.lastName.length")
        ),
      "email" -> text("recipientDetails.error.email.required")
        .verifying(regexp(emailRegex, "recipientDetails.error.email.invalid")),
      "phoneNumber" -> text("recipientDetails.error.phoneNumber.required")
        .verifying(regexp(phoneRegex, "recipientDetails.error.phoneNumber.invalid")),
      "nationalInsuranceNumber" -> text(
        "recipientDetails.error.nationalInsuranceNumber.required"
      )
        .transform[String](_.replaceAll("\\s", "").toUpperCase, identity)
        .verifying(
          firstError(
            regexp(
              ninoFormatRegex,
              "recipientDetails.error.nationalInsuranceNumber.format"
            ),
            regexp(
              ninoValidRegex,
              "recipientDetails.error.nationalInsuranceNumber.invalid"
            )
          )
        ),
      "hasRelatedCase"      -> boolean("recipientDetails.error.hasRelatedCase.required"),
      "caseReferenceNumber" -> optional(
        text().verifying(
          maxLength(
            caseRefMaxLength,
            "recipientDetails.error.caseReferenceNumber.length"
          )
        )
      )
    )(RecipientDetails.apply)(rd =>
      Some(
        (
          rd.firstName,
          rd.lastName,
          rd.email,
          rd.phoneNumber,
          rd.nationalInsuranceNumber,
          rd.hasRelatedCase,
          rd.caseReferenceNumber
        )
      )
    ).verifying(caseReferenceRequiredWhenRelated)
  )
}
