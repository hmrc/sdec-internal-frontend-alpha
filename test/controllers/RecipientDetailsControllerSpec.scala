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

package controllers

import base.SpecBase
import models.RecipientDetails
import pages.RecipientDetailsPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.createthread.RecipientDetailsView

class RecipientDetailsControllerSpec extends SpecBase {

  private val recipientDetails = RecipientDetails(
    firstName = "Jane",
    lastName = "Smith",
    email = "jane.smith@example.com",
    phoneNumber = "07123456789",
    nationalInsuranceNumber = "AB123456C",
    hasRelatedCase = true,
    caseReferenceNumber = Some("CASE-001")
  )

  private val validFormData = Seq(
    "firstName"               -> "Jane",
    "lastName"                -> "Smith",
    "email"                   -> "jane.smith@example.com",
    "phoneNumber"             -> "07123456789",
    "nationalInsuranceNumber" -> "AB123456C",
    "hasRelatedCase"          -> "true",
    "caseReferenceNumber"     -> "CASE-001"
  )

  private val controllerRoutes =
    controllers.createthread.routes.RecipientDetailsController

  "RecipientDetailsController" - {

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllerRoutes.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK

        application.injector.instanceOf[RecipientDetailsView]
      }
    }

    "must return OK and retain previously saved recipient details for a GET" in {
      val userAnswers =
        emptyUserAnswers
          .set(RecipientDetailsPage, recipientDetails)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllerRoutes.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Jane")
        contentAsString(result) must include("Smith")
        contentAsString(result) must include("jane.smith@example.com")
        contentAsString(result) must include("CASE-001")
      }
    }

    "must return Bad Request when invalid data is submitted" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllerRoutes.onSubmit().url)
            .withFormUrlEncodedBody(
              "firstName"               -> "",
              "lastName"                -> "",
              "email"                   -> "not-an-email",
              "phoneNumber"             -> "invalid",
              "nationalInsuranceNumber" -> "invalid",
              "hasRelatedCase"          -> "true",
              "caseReferenceNumber"     -> ""
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return Bad Request when a related case is selected without a case reference" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllerRoutes.onSubmit().url)
            .withFormUrlEncodedBody(
              "firstName"               -> "Jane",
              "lastName"                -> "Smith",
              "email"                   -> "jane.smith@example.com",
              "phoneNumber"             -> "07123456789",
              "nationalInsuranceNumber" -> "AB123456C",
              "hasRelatedCase"          -> "true",
              "caseReferenceNumber"     -> ""
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("caseReferenceNumber")
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllerRoutes.onSubmit().url)
            .withFormUrlEncodedBody(validFormData*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must redirect to the next page and create UserAnswers when no existing answers are present" in {
      val application =
        applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, controllerRoutes.onSubmit().url)
            .withFormUrlEncodedBody(validFormData*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must redirect to the next page when valid data is submitted with existing answers" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllerRoutes.onSubmit().url)
            .withFormUrlEncodedBody(validFormData*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

  }
}
