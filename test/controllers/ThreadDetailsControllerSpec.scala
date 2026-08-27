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
import models.{RecipientDetails, ThreadDetails}
import pages.{RecipientDetailsPage, ThreadDetailsPage}
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.createthread.ThreadDetailsView

import java.time.LocalDate

class ThreadDetailsControllerSpec extends SpecBase {

  private val recipientDetails = RecipientDetails(
    firstName = "Jane",
    lastName = "Smith",
    email = "jane.smith@example.com",
    phoneNumber = "07123456789",
    nationalInsuranceNumber = "PG123456",
    hasRelatedCase = false,
    caseReferenceNumber = None
  )

  private val threadDetails = ThreadDetails(
    message = "Please provide the requested information.",
    responseDate = LocalDate.now().plusDays(7)
  )

  private val validFormData = Map(
    "message"            -> "Please provide the requested information.",
    "responseDate.day"   -> threadDetails.responseDate.getDayOfMonth.toString,
    "responseDate.month" -> threadDetails.responseDate.getMonthValue.toString,
    "responseDate.year"  -> threadDetails.responseDate.getYear.toString
  )

  "ThreadDetailsController" - {

    "must return OK and the correct view for a GET when recipient details exist" in {
      val userAnswers =
        emptyUserAnswers
          .set(RecipientDetailsPage, recipientDetails)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.createthread.routes.ThreadDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual Status.OK

        application.injector.instanceOf[ThreadDetailsView]
      }
    }

    "must redirect to Recipient Details when no recipient details exist for a GET" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.createthread.routes.ThreadDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual Status.SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.createthread.routes.RecipientDetailsController.onPageLoad().url
      }
    }

    "must return OK for a GET when thread details have previously been saved" in {
      val userAnswers =
        emptyUserAnswers
          .set(RecipientDetailsPage, recipientDetails)
          .flatMap(_.set(ThreadDetailsPage, threadDetails))
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.createthread.routes.ThreadDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual Status.OK

        contentAsString(result) must include(threadDetails.message)
      }
    }

    "must return Bad Request when invalid data is submitted" in {
      val userAnswers =
        emptyUserAnswers
          .set(RecipientDetailsPage, recipientDetails)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.createthread.routes.ThreadDetailsController.onSubmit().url)
            .withFormUrlEncodedBody(
              "message"            -> "",
              "responseDate.day"   -> "",
              "responseDate.month" -> "",
              "responseDate.year"  -> ""
            )

        val result = route(application, request).value

        status(result) mustEqual Status.BAD_REQUEST
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val userAnswers =
        emptyUserAnswers
          .set(RecipientDetailsPage, recipientDetails)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.createthread.routes.ThreadDetailsController.onSubmit().url)
            .withFormUrlEncodedBody(validFormData.toSeq*)

        val result = route(application, request).value

        status(result) mustEqual Status.SEE_OTHER
      }
    }

    "must redirect to Recipient Details when there are no recipient details for a POST" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.createthread.routes.ThreadDetailsController.onSubmit().url)
            .withFormUrlEncodedBody(validFormData.toSeq*)

        val result = route(application, request).value

        status(result) mustEqual Status.SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.createthread.routes.RecipientDetailsController.onPageLoad().url
      }
    }
  }
}
