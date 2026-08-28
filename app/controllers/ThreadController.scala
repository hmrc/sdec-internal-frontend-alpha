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

import connectors.ThreadConnector
import controllers.actions.IdentifierAction
import handlers.ErrorHandler
import models.ThreadReference
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.ThreadDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ThreadController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify:                 IdentifierAction,
  threadConnector:          ThreadConnector,
  errorHandler:             ErrorHandler,
  view:                     ThreadDetailsView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(threadReference: ThreadReference): Action[AnyContent] = identify.async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    threadConnector
      .get(threadReference)
      .flatMap {
        case Some(thread) =>
          Future.successful(Ok(view(thread)(using request, request2Messages(request))))
        case None =>
          errorHandler.notFoundTemplate(using request).map(NotFound(_))
      }
      .recover { case NonFatal(exception) =>
        logger.error(s"Failed to load Thread (ref: ${threadReference.value})", exception)
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }

  }

}
