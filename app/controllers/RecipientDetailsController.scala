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

import controllers.actions.{
  DataRequiredAction,
  DataRetrievalAction,
  IdentifierAction
}
import forms.RecipientDetailsFormProvider
import models.{NormalMode, RecipientDetails, UserAnswers}
import navigation.Navigator
import pages.RecipientDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RecipientDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RecipientDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: RecipientDetailsFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: RecipientDetailsView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form: Form[RecipientDetails] = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData) { request =>
      given Request[AnyContent] = request

      val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

      val preparedForm = userAnswers.get(RecipientDetailsPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData).async { request =>
      given Request[AnyContent] = request

      form
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[RecipientDetails]) =>
            Future
              .successful(BadRequest(view(remapCaseReferenceError(formWithErrors)))),
          value => {
            val userAnswers =
              request.userAnswers.getOrElse(UserAnswers(request.userId))

            for {
              updatedAnswers <- Future
                .fromTry(userAnswers.set(RecipientDetailsPage, value))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(RecipientDetailsPage, NormalMode, updatedAnswers)
            )
          }
        )
    }

  private def remapCaseReferenceError(
      form: Form[RecipientDetails]
  ): Form[RecipientDetails] =
    form.copy(errors = form.errors.map { e =>
      if (e.key.isEmpty) e.copy(key = "caseReferenceNumber") else e
    })
}
