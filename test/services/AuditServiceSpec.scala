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

import base.StrideAuthUserFixtures
import models.{RecipientDetails, ThreadDetails, ThreadReference}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterEach
    with StrideAuthUserFixtures {

  given HeaderCarrier    = HeaderCarrier()
  given ExecutionContext = ExecutionContext.global

  private val auditSource     = "test-audit-source"
  private val threadReference = "THREAD1000AA"
  private val auditFailure    = AuditResult.Failure("Event sending failed")

  private val recipientDetails = RecipientDetails(
    firstName = "John",
    lastName = "Smith",
    email = "john.smith@example.com",
    phoneNumber = "07123456789",
    nationalInsuranceNumber = "AA000000A",
    hasRelatedCase = true,
    caseReferenceNumber = Some("CASE-001")
  )

  private val threadDetails = ThreadDetails(
    message = "Please send your payslips for John Smith",
    responseDate = LocalDate.of(2026, 11, 20)
  )

  private val mockAuditConnector: AuditConnector = mock[AuditConnector]
  private val auditService:       AuditService   = AuditService(mockAuditConnector)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAuditConnector)
    when(mockAuditConnector.auditingConfig)
      .thenReturn(AuditingConfig(consumer = None, enabled = true, auditSource = auditSource, auditSentHeaders = false))
    when(mockAuditConnector.sendExtendedEvent(any())(using any(), any()))
      .thenReturn(Future.successful(AuditResult.Success))
  }

  private def capturedEvent(): ExtendedDataEvent = {
    val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
    verify(mockAuditConnector).sendExtendedEvent(captor.capture())(using any(), any())
    captor.getValue
  }

  private def connectorReturns(result: AuditResult): Unit =
    when(mockAuditConnector.sendExtendedEvent(any())(using any(), any()))
      .thenReturn(Future.successful(result))

  "AuditService" - {
    "auditThreadCreated" - {
      "must send a ThreadCreated event with the audit source from config" in {
        auditService
          .auditThreadCreated(testStrideAuthUser, threadReference, recipientDetails, threadDetails)
          .futureValue mustBe AuditResult.Success

        val event = capturedEvent()
        event.auditSource mustBe auditSource
        event.auditType mustBe "ThreadCreated"
        event.detail mustBe Json.obj(
          "staffPid"        -> "test-provider-id",
          "role"            -> "SDEC_VAT_User",
          "threadReference" -> threadReference,
          "firstName"       -> recipientDetails.firstName,
          "lastName"        -> recipientDetails.lastName,
          "email"           -> recipientDetails.email,
          "nino"            -> recipientDetails.nationalInsuranceNumber,
          "hasRelatedCase"  -> true,
          "responseDate"    -> "2026-11-20"
        )
      }

      "must return the result from the connector unchanged" in {
        connectorReturns(auditFailure)

        auditService
          .auditThreadCreated(testStrideAuthUser, threadReference, recipientDetails, threadDetails)
          .futureValue mustBe auditFailure
      }
    }

    "auditThreadViewed" - {
      "must send a ThreadViewed event with the audit source from config" in {
        auditService
          .auditThreadViewed(testStrideAuthUser, ThreadReference(threadReference))
          .futureValue mustBe AuditResult.Success

        val event = capturedEvent()
        event.auditSource mustBe auditSource
        event.auditType mustBe "ThreadViewed"
        event.detail mustBe Json.obj(
          "staffPid"        -> "test-provider-id",
          "role"            -> "SDEC_VAT_User",
          "threadReference" -> threadReference
        )
      }

      "must send 'unknown' as the role when the user has no enrolments" in {
        val userWithoutRole = createTestStrideAuthUser(allEnrolments = Set.empty[Enrolment])

        auditService.auditThreadViewed(userWithoutRole, ThreadReference(threadReference)).futureValue

        (capturedEvent().detail \ "role").as[String] mustBe "unknown"
      }

      "must return the result from the connector unchanged" in {
        connectorReturns(auditFailure)

        auditService
          .auditThreadViewed(testStrideAuthUser, ThreadReference(threadReference))
          .futureValue mustBe auditFailure
      }
    }

    "auditWorkspaceAccessDenied" - {
      "must send a WorkspaceAccessDenied event with the audit source from config" in {
        auditService.auditWorkspaceAccessDenied(testStrideAuthUser).futureValue mustBe AuditResult.Success

        val event = capturedEvent()
        event.auditSource mustBe auditSource
        event.auditType mustBe "WorkspaceAccessDenied"
        event.detail mustBe Json.obj(
          "staffPid" -> "test-provider-id",
          "role"     -> "SDEC_VAT_User"
        )
      }

      "must return the result from the connector unchanged" in {
        connectorReturns(auditFailure)

        auditService.auditWorkspaceAccessDenied(testStrideAuthUser).futureValue mustBe auditFailure
      }
    }
  }

}
