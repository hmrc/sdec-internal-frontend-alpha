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

import models.{RecipientDetails, ThreadDetails, ThreadReference}
import stride.StrideAuthUser
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.Future

trait AuditServiceAlgebra {

  def auditThreadCreated(
    user:             StrideAuthUser,
    threadReference:  String,
    recipientDetails: RecipientDetails,
    threadDetails:    ThreadDetails
  )(using hc: HeaderCarrier): Future[AuditResult]

  def auditThreadViewed(
    user:            StrideAuthUser,
    threadReference: ThreadReference
  )(using hc: HeaderCarrier): Future[AuditResult]

  def auditWorkspaceAccessDenied(
    user: StrideAuthUser
  )(using hc: HeaderCarrier): Future[AuditResult]

}
