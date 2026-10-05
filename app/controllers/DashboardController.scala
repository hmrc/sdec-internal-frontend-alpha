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

import connectors.StaffConnector
import models.ThreadFilter
import models.requests.StaffAccessRequest
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DashboardService
import stride.StrideAuthAlgebra
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.DashboardView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DashboardController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  strideAuth:               StrideAuthAlgebra,
  dashboardService:         DashboardService,
  staffConnector:           StaffConnector,
  view:                     DashboardView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(filter: Option[ThreadFilter]): Action[AnyContent] =
    strideAuth.authorisedFromStride { (strideUser, request) =>
      val pid  = strideUser.credentials.providerId
      val role = strideUser.allEnrollments.enrolments.head.key

      given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      logger.info(s"STRIDE User [$strideUser]")

      val accessRequest =
        StaffAccessRequest(
          pid = pid,
          role = role
        )

      staffConnector
        .validateAccess(accessRequest)
        .flatMap { accessResponse =>
          if accessResponse.authorised then
            dashboardService
              .getDashboard(userId = pid, selectedFilter = filter)
              .map(dashboard => Ok(view(dashboard)(using request, request2Messages(request))))
          else Future.successful(Redirect(routes.DevelopmentInProgressController.onPageLoad()))
        }
        .recover { case NonFatal(exception) =>
          logger.error("Failed to load the Workspace", exception)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }

}
