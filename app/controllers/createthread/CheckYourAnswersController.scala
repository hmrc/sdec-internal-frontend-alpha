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

package controllers.createthread

import com.google.inject.Inject
import connectors.ThreadCreateConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.CreateThreadRequest
import pages.{RecipientDetailsPage, ThreadDetailsPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.createthread.CheckYourAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.createthread.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify:                 IdentifierAction,
  getData:                  DataRetrievalAction,
  requireData:              DataRequiredAction,
  checkYourAnswersService:  CheckYourAnswersService,
  val controllerComponents: MessagesControllerComponents,
  view:                     CheckYourAnswersView,
  threadCreateConnector:    ThreadCreateConnector,
  sessionRepository:        SessionRepository
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { request =>
    given Request[AnyContent] = request

    (request.userAnswers.get(RecipientDetailsPage), request.userAnswers.get(ThreadDetailsPage)) match {
      case (Some(recipient), Some(threadDetails)) =>
        Ok(
          view(
            checkYourAnswersService.recipientDetailsList(recipient),
            checkYourAnswersService.threadDetailsList(threadDetails)
          )
        )
      case _ =>
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { request =>
      given Request[AnyContent] = request

      (
        request.userAnswers.get(RecipientDetailsPage),
        request.userAnswers.get(ThreadDetailsPage)
      ) match {

        case (Some(recipient), Some(threadDetails)) =>

          val createThreadRequest =
            CreateThreadRequest(
              recipientDetails = recipient,
              threadDetails = threadDetails
            )

          threadCreateConnector
            .createThread(createThreadRequest)
            .flatMap { response =>
              for {
                cleared <- Future.fromTry(
                             request.userAnswers
                               .remove(RecipientDetailsPage)
                               .flatMap(_.remove(ThreadDetailsPage))
                           )
                _ <- sessionRepository.set(cleared)
              } yield Redirect(controllers.createthread.routes.ThreadViewController.onPageLoad(response.threadReference))
            }

        case _ =>
          Future.successful(
            Redirect(
              controllers.routes.JourneyRecoveryController.onPageLoad()
            )
          )
      }
    }
}
