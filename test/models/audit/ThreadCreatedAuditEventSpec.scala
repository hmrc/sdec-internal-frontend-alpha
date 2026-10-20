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

package models.audit

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.LocalDate

class ThreadCreatedAuditEventSpec extends AnyFreeSpec with Matchers {

  private val threadCreatedAuditEvent = ThreadCreatedAuditEvent(
    staffPid = "1234567",
    role = "SDEC_VAT_User",
    threadReference = "THREAD1000AA",
    hasRelatedCase = true,
    responseDate = LocalDate.of(2026, 11, 20)
  )

  "ThreadCreatedAuditEvent" - {
    "auditType" - {
      "must be ThreadCreated" in {
        threadCreatedAuditEvent.auditType mustBe "ThreadCreated"
      }
    }

    "detail" - {
      "must contain only the agreed fields" in {
        threadCreatedAuditEvent.detail mustBe Json.obj(
          "staffPid"        -> "1234567",
          "role"            -> "SDEC_VAT_User",
          "threadReference" -> "THREAD1000AA",
          "hasRelatedCase"  -> true,
          "responseDate"    -> "2026-11-20"
        )
      }
    }

    "extendedDataEvent" - {
      "must carry the given audit source, the audit type and the detail" in {
        val dataEvent = threadCreatedAuditEvent.extendedDataEvent("test-audit-source")

        dataEvent.auditSource mustBe "test-audit-source"
        dataEvent.auditType mustBe threadCreatedAuditEvent.auditType
        dataEvent.detail mustBe threadCreatedAuditEvent.detail
      }
    }
  }
}
