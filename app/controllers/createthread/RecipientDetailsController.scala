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
import controllers.routes
import forms.RecipientDetailsFormProvider
import models.requests.IdentifierRequest.identifierRequest
import models.requests.OptionalDataRequest
import models.{NormalMode, RecipientDetails, UserAnswers}
import navigation.Navigator
import pages.RecipientDetailsPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import stride.StrideAuthAlgebra
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.createthread.RecipientDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RecipientDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository:        SessionRepository,
  navigator:                Navigator,
  identify:                 IdentifierAction,
  strideAuth:               StrideAuthAlgebra,
  getData:                  DataRetrievalAction,
  formProvider:             RecipientDetailsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view:                     RecipientDetailsView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form: Form[RecipientDetails] = formProvider()

  def onPageLoad(): Action[AnyContent] =
    // We are using this method as there may not be any form data stored
    strideAuth.authorisedFromStride { (user, request) =>
      given Request[AnyContent] = request

      val idRequest = identifierRequest(user, request)

      getData.retrieve(idRequest).map { requestWithData =>
        val userAnswers =
          requestWithData.userAnswers.getOrElse(
            UserAnswers(idRequest.userId)
          )

        val preparedForm =
          userAnswers.get(RecipientDetailsPage) match {
            case Some(value) => form.fill(value)
            case None        => form
          }

        Ok(view(preparedForm))
      }
    }

  def onSubmit(): Action[AnyContent] =
    logger.info(s"Handling Recipient Details Submission")
    strideAuth.authorisedFromStride { (user, dataRequest) =>
      given Request[AnyContent] = dataRequest
      val idRequest             = identifierRequest(user, dataRequest)
      getData.retrieve(idRequest).flatMap(processForm)
    }

  private def processForm(
    dataRequest: OptionalDataRequest[AnyContent]
  )(using Request[AnyContent]): Future[Result] =
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(remapCaseReferenceError(formWithErrors)))
          ),
        recipientDetails => saveRecipientDetails(dataRequest, recipientDetails)
      )

  private def saveRecipientDetails(
    request:          OptionalDataRequest[AnyContent],
    recipientDetails: RecipientDetails
  ): Future[Result] =
    (for {
      updatedAnswers <- Future.fromTry(
                          request.userAnswers
                            .getOrElse(UserAnswers(request.userId))
                            .set(RecipientDetailsPage, recipientDetails)
                        )
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(
      navigator.nextPage(
        RecipientDetailsPage,
        NormalMode,
        updatedAnswers
      )
    )).recover { case NonFatal(exception) =>
      logger.error("Failed to save the recipient details", exception)
      Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

  private def remapCaseReferenceError(form: Form[RecipientDetails]): Form[RecipientDetails] =
    form.copy(errors = form.errors.map { e =>
      if e.key.isEmpty then e.copy(key = "caseReferenceNumber") else e
    })
}
