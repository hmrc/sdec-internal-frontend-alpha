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

package service

import models.Thread
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import services.DashboardService
import viewmodels.{DashboardThread, ThreadPriority}

import java.time.{Clock, LocalDate, ZoneOffset}

class DashboardServiceSpec extends AnyWordSpec with Matchers {

  private val today = LocalDate.of(2026, 8, 26)

  private val clock: Clock =
    Clock.fixed(
      today.atStartOfDay(ZoneOffset.UTC).toInstant,
      ZoneOffset.UTC
    )

  private val service = new DashboardService(clock)

  "buildThreads" should {

    "return an empty sequence when no threads are provided" in {
      service.buildThreads(Seq.empty) shouldBe Seq.empty
    }

    "map a thread to a dashboard thread" in {
      val thread = Thread(
        threadReference = "THREAD001",
        relatedReference = Some("CASE001"),
        externalContact = "Jane Smith",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(LocalDate.of(2026, 8, 30))
      )

      service.buildThreads(Seq(thread)) shouldBe Seq(
        DashboardThread(
          threadReference = "THREAD001",
          relatedReference = "CASE001",
          externalContact = "Jane Smith",
          status = "Open",
          waitingOn = "HMRC",
          deadline = "30/08/2026",
          priority = ThreadPriority.None
        )
      )
    }

    "use a hyphen when the related reference is missing" in {
      val thread = Thread(
        threadReference = "THREAD002",
        relatedReference = None,
        externalContact = "John Smith",
        status = "Open",
        waitingOn = "Recipient",
        deadline = None
      )

      service.buildThreads(Seq(thread)) shouldBe Seq(
        DashboardThread(
          threadReference = "THREAD002",
          relatedReference = "-",
          externalContact = "John Smith",
          status = "Open",
          waitingOn = "Recipient",
          deadline = "-",
          priority = ThreadPriority.None
        )
      )
    }

    "format the deadline as dd/MM/yyyy" in {
      val thread = Thread(
        threadReference = "THREAD003",
        relatedReference = Some("CASE003"),
        externalContact = "Alex Brown",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(LocalDate.of(2026, 1, 5))
      )

      service.buildThreads(Seq(thread)).head.deadline shouldBe "05/01/2026"
    }

    "set priority to Overdue when the deadline is before today" in {
      val thread = Thread(
        threadReference = "THREAD004",
        relatedReference = Some("CASE004"),
        externalContact = "Alex Brown",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(today.minusDays(1))
      )

      service.buildThreads(Seq(thread)).head.priority shouldBe ThreadPriority.Overdue
    }

    "set priority to ResponseReceived when the status is Needs action" in {
      val thread = Thread(
        threadReference = "THREAD005",
        relatedReference = Some("CASE005"),
        externalContact = "Alex Brown",
        status = "Needs action",
        waitingOn = "HMRC",
        deadline = Some(today.plusDays(1))
      )

      service.buildThreads(Seq(thread)).head.priority shouldBe
        ThreadPriority.ResponseReceived
    }

    "prioritise an overdue thread as Overdue even when its status is Needs action" in {
      val thread = Thread(
        threadReference = "THREAD006",
        relatedReference = Some("CASE006"),
        externalContact = "Alex Brown",
        status = "Needs action",
        waitingOn = "HMRC",
        deadline = Some(today.minusDays(1))
      )

      service.buildThreads(Seq(thread)).head.priority shouldBe ThreadPriority.Overdue
    }

    "set priority to None when the deadline is today" in {
      val thread = Thread(
        threadReference = "THREAD007",
        relatedReference = Some("CASE007"),
        externalContact = "Alex Brown",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(today)
      )

      service.buildThreads(Seq(thread)).head.priority shouldBe ThreadPriority.None
    }
  }
}
