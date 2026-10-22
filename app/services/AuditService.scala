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

package services

import models.audit.{ThreadCreatedAuditEvent, ThreadViewedAuditEvent, WorkspaceAccessDeniedAuditEvent}
import models.{RecipientDetails, ThreadDetails, ThreadReference}
import play.api.Logging
import stride.StrideAuthUser
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (auditConnector: AuditConnector)(using ec: ExecutionContext)
    extends AuditServiceAlgebra
    with Logging {

  private def auditSource: String = auditConnector.auditingConfig.auditSource

  private def roleOf(user: StrideAuthUser): String =
    user.allEnrollments.enrolments.headOption.map(_.key).getOrElse("unknown")

  override def auditThreadCreated(
    user:             StrideAuthUser,
    threadReference:  String,
    recipientDetails: RecipientDetails,
    threadDetails:    ThreadDetails
  )(using hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ThreadCreatedAuditEvent(
        staffPid = user.credentials.providerId,
        role = roleOf(user),
        threadReference = threadReference,
        firstName = recipientDetails.firstName,
        lastName = recipientDetails.lastName,
        email = recipientDetails.email,
        nino = recipientDetails.nationalInsuranceNumber,
        hasRelatedCase = recipientDetails.hasRelatedCase,
        responseDate = threadDetails.responseDate
      ).extendedDataEvent(auditSource)
    )

  override def auditThreadViewed(
    user:            StrideAuthUser,
    threadReference: ThreadReference
  )(using hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ThreadViewedAuditEvent(
        staffPid = user.credentials.providerId,
        role = roleOf(user),
        threadReference = threadReference.value
      ).extendedDataEvent(auditSource)
    )

  override def auditWorkspaceAccessDenied(
    user: StrideAuthUser
  )(using hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      WorkspaceAccessDeniedAuditEvent(
        staffPid = user.credentials.providerId,
        role = roleOf(user)
      ).extendedDataEvent(auditSource)
    )
}
