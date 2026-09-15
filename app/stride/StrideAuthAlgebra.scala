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

package stride

import config.FrontendAppConfig
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, InsufficientEnrolments, NoActiveSession}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait StrideAuthAlgebra {
  def authorisedFromStride(
    action: (StrideAuthUser, Request[AnyContent]) => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent]
}

class StrideAuth @Inject() (
  val authConnector: AuthConnector,
  actionBuilder:     DefaultActionBuilder,
  config:            FrontendAppConfig
) extends StrideAuthAlgebra
    with AuthorisedFunctions
    with Results
    with FrontendHeaderCarrierProvider
    with Logging {

  override def authorisedFromStride(action: (StrideAuthUser, Request[AnyContent]) => Future[Result])(implicit
    ec: ExecutionContext
  ): Action[AnyContent] =
    actionBuilder.async { request =>
      given givenRequest: Request[AnyContent] = request
      authorised(AuthProviders(PrivilegedApplication))
        .retrieve(
          Retrievals.credentials
            .and(Retrievals.email)
            .and(Retrievals.authorisedEnrolments)
            .and(Retrievals.allEnrolments)
            .and(Retrievals.name)
        ) { case credentials ~ email ~ authorisedEnrollments ~ allEnrollments ~ name =>
          val strideUser = StrideAuthUser(credentials, email, authorisedEnrollments, allEnrollments, name)
          logger.info(s"====================================================")
          logger.info(s"Internal Staff: ${strideUser.name}")
          logger.info(s"Email Address: ${strideUser.email}")
          logger.info(s"Credentials: ${strideUser.credentials}")
          logger.info(s"Authorised Enrollments: ${strideUser.authorisedEnrollments.enrolments.mkString(",")}")
          logger.info(s"All Enrollments: ${strideUser.allEnrollments.enrolments.mkString(",")}")
          logger.info(s"====================================================")
          action(strideUser, request)
        }
        .recoverWith {
          case e: NoActiveSession =>
            logger.warn(s"No active session: ${e.reason}")
            Future.successful(
              Redirect(
                config.loginUrl,
                Map("successURL" -> Seq(config.loginContinueUrl), "origin" -> Seq(config.appName))
              )
            )
          case e: InsufficientEnrolments =>
            logger.info(s"Insufficient enrollments: ${e.msg}")
            Future.successful(
              SeeOther(controllers.routes.InsufficientRolesController.get.url)
            )
        }
    }
}
