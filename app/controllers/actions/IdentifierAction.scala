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
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

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

  private val teamIdKey   = "teamId"
  private val teamNameKey = "teamName"
  private val userNameKey = "userName"

  private val strideParams: Map[String, Seq[String]] =
    Map(
      "successURL" -> Seq(config.loginContinueUrl),
      "origin"     -> Seq(config.appName)
    )

  override def invokeBlock[A](
    request: Request[A],
    block:   IdentifierRequest[A] => Future[Result]
  ): Future[Result] = {

    given HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(AuthProviders(PrivilegedApplication))
      .retrieve(Retrievals.credentials and Retrievals.name and Retrievals.allEnrolments) {

        case Some(credentials) ~ name ~ enrolments =>
          val userId   = credentials.providerId
          val userName = displayName(name, userId)

          team(cachedTeam(request), enrolments, userId).flatMap {
            case Some(team) =>
              block(IdentifierRequest(withSession(request, userName, team), userId, userName, team.id))
            case None =>
              Future.successful(Redirect(routes.InsufficientRolesController.onPageLoad()))
          }

        case None ~ _ ~ _ =>
          logger.warn("No STRIDE credentials returned")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, strideParams)
        case _: UnsupportedAuthProvider =>
          logger.warn("Non-STRIDE session presented, redirecting to STRIDE sign in")
          Redirect(config.loginUrl, strideParams)
        case e: AuthorisationException =>
          logger.warn(s"Authorisation failed: ${e.reason}")
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
  }

  private def displayName(name: Option[Name], fallback: String): String =
    name
      .map(n => Seq(n.name, n.lastName).flatten.mkString(" "))
      .filter(_.nonEmpty)
      .getOrElse(fallback)

  private def cachedTeam[A](request: Request[A]): Option[TeamRef] =
    for {
      id   <- request.session.get(teamIdKey)
      name <- request.session.get(teamNameKey)
    } yield TeamRef(id, name)

  private def team(
    cached:     Option[TeamRef],
    enrolments: Enrolments,
    userId:     String
  )(using HeaderCarrier): Future[Option[TeamRef]] =
    cached match {
      case Some(_) => Future.successful(cached)
      case None    => validatedTeam(enrolments, userId)
    }

  private def validatedTeam(
    enrolments: Enrolments,
    userId:     String
  )(using HeaderCarrier): Future[Option[TeamRef]] =
    sdecRole(enrolments) match {

      case None =>
        logger.warn(s"No ${config.strideRolePrefix}* role found for user $userId")
        Future.successful(None)

      case Some(role) =>
        teamsConnector.getTeamByRole(role).map { team =>
          if team.isEmpty then logger.warn(s"Stride role $role does not map to a known team")
          team.map(t => TeamRef(t.id, t.name))
        }
    }

  private def sdecRole(enrolments: Enrolments): Option[String] =
    enrolments.enrolments
      .map(_.key)
      .filter(_.toLowerCase.startsWith(config.strideRolePrefix.toLowerCase))
      .toSeq
      .sorted
      .headOption

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
