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
import connectors.ThreadConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DashboardControllerSpec extends SpecBase {

  private def baseApplication = applicationBuilder(userAnswers = None)
    .overrides(
      bind[ThreadConnector].toInstance(mockThreadConnector)
    )

  "Dashboard Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = baseApplication.build()

      when(mockThreadConnector.getAll()(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Seq.empty))

      running(application) {
        val request = FakeRequest(GET, routes.DashboardController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery when loading threads fails" in {
      val application = baseApplication.build()

      val exception = new RuntimeException("Unable to load threads")

      when(mockThreadSummaryConnector.getAll()(using any[HeaderCarrier]))
        .thenReturn(Future.failed(exception))

      running(application) {
        val request =
          FakeRequest(GET, routes.DashboardController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
