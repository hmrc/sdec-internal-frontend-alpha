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
import connectors.{TeamsConnector, ThreadCreateConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.*
import models.requests.CreateThreadRequest
import pages.{RecipientDetailsPage, ThreadDetailsPage}
import play.api.Logging
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
  sessionRepository:        SessionRepository,
  teamsConnector:           TeamsConnector
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

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

      val creator = UserRef(request.userId, request.userName)

      val result =
        for {
          recipient     <- request.userAnswers.get(RecipientDetailsPage)
          threadDetails <- request.userAnswers.get(ThreadDetailsPage)
        } yield teamsConnector.getTeam(request.teamId).flatMap {
          case Some(team) =>
            createThread(
              creator,
              team.ownerFor(creator),
              team,
              recipient,
              threadDetails,
              request.userAnswers
            )
          case None =>
            logger.error(s"Team ${request.teamId} could not be resolved at submit")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }

      result.getOrElse(
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      )
    }

  private def createThread(
    threadCreator:    UserRef,
    threadOwner:      Option[UserRef],
    owningTeam:       Team,
    recipientDetails: RecipientDetails,
    threadDetails:    ThreadDetails,
    userAnswers:      UserAnswers
  )(using Request[AnyContent]): Future[Result] = {

    val createThreadRequest =
      CreateThreadRequest(
        threadCreator = threadCreator,
        threadOwner = threadOwner,
        owningTeam = owningTeam,
        recipientDetails = recipientDetails,
        threadDetails = threadDetails
      )

    for {
      response <- threadCreateConnector.createThread(createThreadRequest)
      cleared  <- Future.fromTry(
                   userAnswers
                     .remove(RecipientDetailsPage)
                     .flatMap(_.remove(ThreadDetailsPage))
                 )
      _ <- sessionRepository.set(cleared)
    } yield Redirect(
                controllers.createthread.routes.ThreadViewController.onPageLoad(response.threadReference)
              )
      .flashing("confirmationBanner" -> "true")
  }
}
