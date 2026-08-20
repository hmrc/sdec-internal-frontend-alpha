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

import connectors.ThreadSummaryConnector
import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DashboardService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.DashboardView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class DashboardController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    threadSummaryConnector: ThreadSummaryConnector,
    dashboardService: DashboardService,
    view: DashboardView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging:

    def onPageLoad(): Action[AnyContent] = identify.async { request =>
      given HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      threadSummaryConnector
        .getAll()
        .map { threads =>
          val dashboardThreads = dashboardService.buildThreads(threads)
          Ok(
            view(dashboardThreads)(using
              request,
              request2Messages(request)
            )
          )
        }
        .recover { case NonFatal(exception) =>
          logger.error("Failed to load the Workspace", exception)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
