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
import config.FrontendAppConfig
import connectors.ThreadCreateConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.{CreateThreadRequest, DataRequest}
import models.{RecipientDetails, Team, ThreadDetails}
import pages.{RecipientDetailsPage, ThreadDetailsPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.createthread.CheckYourAnswersService
import stride.{StrideAuthAlgebra, StrideAuthUser}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.createthread.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify:                 IdentifierAction,
  strideAuth:               StrideAuthAlgebra,
  getData:                  DataRetrievalAction,
  requireData:              DataRequiredAction,
  checkYourAnswersService:  CheckYourAnswersService,
  val controllerComponents: MessagesControllerComponents,
  view:                     CheckYourAnswersView,
  threadCreateConnector:    ThreadCreateConnector,
  sessionRepository:        SessionRepository,
  appConfig:                FrontendAppConfig
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] =
    strideAuth.authorisedFromStrideWithData { (_, request) =>
      given Request[AnyContent] = request

      (request.userAnswers.get(RecipientDetailsPage), request.userAnswers.get(ThreadDetailsPage)) match {
        case (Some(recipient), Some(threadDetails)) =>
          Future.successful(
            Ok(
              view(
                checkYourAnswersService.recipientDetailsList(recipient),
                checkYourAnswersService.threadDetailsList(threadDetails)
              )
            )
          )
        case _ =>
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
    }

  def onSubmit(): Action[AnyContent] =
    strideAuth.authorisedFromStrideWithData { (user, dataRequest) =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequest(dataRequest.request)

      given Request[AnyContent] = dataRequest

      (
        dataRequest.userAnswers.get(RecipientDetailsPage),
        dataRequest.userAnswers.get(ThreadDetailsPage)
      ) match {
        case (Some(recipient), Some(threadDetails)) =>
          createThread(user, dataRequest, recipient, threadDetails)
        case _ =>
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
    }

  private def createThread(
    user:          StrideAuthUser,
    dataRequest:   DataRequest[AnyContent],
    recipient:     RecipientDetails,
    threadDetails: ThreadDetails
  )(using HeaderCarrier): Future[Result] = {
    val createThreadRequest =
      CreateThreadRequest(
        threadCreator = user.credentials.providerId,
        owningTeam = Team.fromRole(user.allEnrollments.enrolments.head.key),
        recipientDetails = recipient,
        threadDetails = threadDetails
      )
    threadCreateConnector
      .createThread(createThreadRequest)
      .flatMap { response =>
        clearSession(dataRequest).map { _ =>
          Redirect(
            controllers.createthread.routes.ThreadViewController
              .onPageLoad(response.threadReference)
          ).flashing("confirmationBanner" -> "true")
        }
      }
  }

  private def clearSession(
    dataRequest: DataRequest[AnyContent]
  ): Future[Unit] =
    for {
      cleared <- Future.fromTry(
                   dataRequest.userAnswers
                     .remove(RecipientDetailsPage)
                     .flatMap(_.remove(ThreadDetailsPage))
                 )
      _ <- sessionRepository.set(cleared)
    } yield ()
}
