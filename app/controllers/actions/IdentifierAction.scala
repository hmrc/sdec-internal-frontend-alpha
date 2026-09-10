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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.TeamsConnector
import controllers.routes
import models.TeamRef
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.mvc.request.{Cell, RequestAttrKey}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config:                     FrontendAppConfig,
  teamsConnector:             TeamsConnector,
  val parser:                 BodyParsers.Default
)(using ec: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override val executionContext: ExecutionContext = ec

  private val teamIdKey          = "teamId"
  private val userNameKey        = "userName"
  private val teamEnrolmentKey   = "HMRC-SDEC-TEAM"
  private val teamIdentifierName = "TeamId"
  private val teamNameKey        = "teamName"

  override def invokeBlock[A](
    request: Request[A],
    block:   IdentifierRequest[A] => Future[Result]
  ): Future[Result] = {

    given HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId and Retrievals.name and Retrievals.allEnrolments) {
      case Some(internalId) ~ name ~ enrolments =>
        val userName = displayName(name, internalId)

        val cached =
          for {
            id   <- request.session.get(teamIdKey)
            name <- request.session.get(teamNameKey)
          } yield TeamRef(id, name)

        team(cached, enrolments, internalId).flatMap {
          case Some(team) =>
            block(IdentifierRequest(withSession(request, userName, team), internalId, userName, team.id))
          case None =>
            Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
        }

      case None ~ _ ~ _ =>
        throw new UnauthorizedException("Unable to retrieve internal Id")
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }

  private def displayName(name: Option[Name], internalId: String): String =
    name
      .map(n => Seq(n.name, n.lastName).flatten.mkString(" "))
      .filter(_.nonEmpty)
      .getOrElse(internalId)

  private def team(
    cached:     Option[TeamRef],
    enrolments: Enrolments,
    internalId: String
  )(using HeaderCarrier): Future[Option[TeamRef]] =
    cached match {
      case Some(_) => Future.successful(cached)
      case None    => validatedTeam(enrolments, internalId)
    }

  private def validatedTeam(
    enrolments: Enrolments,
    internalId: String
  )(using HeaderCarrier): Future[Option[TeamRef]] =
    enrolments
      .getEnrolment(teamEnrolmentKey)
      .flatMap(_.getIdentifier(teamIdentifierName))
      .map(_.value) match {

      case None =>
        logger.warn(s"No $teamEnrolmentKey enrolment found for user $internalId")
        Future.successful(None)

      case Some(teamId) =>
        teamsConnector.getTeam(teamId).map { team =>
          if team.isEmpty then logger.warn(s"Team id $teamId from enrolment is not a known team")
          team.map(t => TeamRef(t.id, t.name))
        }
    }

  private def withSession[A](request: Request[A], userName: String, team: TeamRef): Request[A] =
    request.addAttr(
      RequestAttrKey.Session,
      Cell(
        request.session
          + (userNameKey -> userName)
          + (teamIdKey   -> team.id)
          + (teamNameKey -> team.name)
      )
    )
}

class SessionIdentifierAction @Inject() (
  val parser: BodyParsers.Default
)(using ec: ExecutionContext)
    extends IdentifierAction {

  override val executionContext: ExecutionContext = ec

  override def invokeBlock[A](
    request: Request[A],
    block:   IdentifierRequest[A] => Future[Result]
  ): Future[Result] = {

    val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    hc.sessionId match {
      case Some(session) =>
        val updatedRequest = request.addAttr(
          RequestAttrKey.Session,
          Cell(request.session + ("userName" -> session.value))
        )

        block(IdentifierRequest(updatedRequest, session.value, session.value, ""))
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
