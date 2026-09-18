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

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import forms.ThreadDetailsFormProvider
import models.{NormalMode, RecipientDetails, ThreadDetails, UserAnswers}
import navigation.Navigator
import pages.{RecipientDetailsPage, ThreadDetailsPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import stride.StrideAuthAlgebra
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.createthread.ThreadDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ThreadDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository:        SessionRepository,
  navigator:                Navigator,
  identify:                 IdentifierAction,
  strideAuth:               StrideAuthAlgebra,
  getData:                  DataRetrievalAction,
  formProvider:             ThreadDetailsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view:                     ThreadDetailsView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def form(implicit request: RequestHeader): Form[ThreadDetails] = {
    given Messages = messagesApi.preferred(request)

    formProvider()
  }

  def onPageLoad(): Action[AnyContent] =
    strideAuth.authorisedFromStrideWithData { (_, dataRequest) =>
      given Request[AnyContent] = dataRequest

      dataRequest.userAnswers.get(RecipientDetailsPage) match {
        case Some(recipient) =>
          val preparedForm = dataRequest.userAnswers.get(ThreadDetailsPage) match {
            case Some(values) => form.fill(values)
            case None         => form
          }
          Future.successful(Ok(view(preparedForm, recipient)))
        case None =>
          logger.info(s"No match found for Recipient Details in ThreadDetailsController")
          Future.successful(
            Redirect(controllers.createthread.routes.RecipientDetailsController.onPageLoad())
          )
      }
    }

  def onSubmit(): Action[AnyContent] =
    strideAuth.authorisedFromStrideWithData { (_, dataRequest) =>
      given Request[AnyContent] = dataRequest

      dataRequest.userAnswers.get(RecipientDetailsPage) match {
        case Some(recipient) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, recipient))),
              threadDetails => saveThreadDetails(threadDetails, dataRequest.userAnswers)
            )
        case None =>
          Future.successful(
            Redirect(
              controllers.createthread.routes.RecipientDetailsController.onPageLoad()
            )
          )
      }
    }

  private def saveThreadDetails(threadDetails: ThreadDetails, answers: UserAnswers): Future[Result] =
    (for {
      updatedAns <- Future.fromTry(answers.set(ThreadDetailsPage, threadDetails))
      _          <- sessionRepository.set(updatedAns)

    } yield Redirect(
      navigator.nextPage(ThreadDetailsPage, NormalMode, updatedAns)
    )).recover { case NonFatal(e) =>
      logger.error(s"Failed to save thread details: ${e.getMessage}")
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

}
