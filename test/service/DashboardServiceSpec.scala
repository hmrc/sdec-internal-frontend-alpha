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

import base.SpecBase
import models.{Thread, ThreadReference}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import services.DashboardService
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{DashboardThread, ThreadPriority}

import java.time.{Clock, LocalDate, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class DashboardServiceSpec()(using ExecutionContext) extends SpecBase {

  private given HeaderCarrier = HeaderCarrier()

  private val today = LocalDate.of(2026, 8, 26)

  private val clock: Clock =
    Clock.fixed(
      today.atStartOfDay(ZoneOffset.UTC).toInstant,
      ZoneOffset.UTC
    )

  private val service = new DashboardService(mockThreadConnector, clock)

  "getDashboardThreads" - {

    "return an empty sequence when no threads are provided" in {
      dashboardThreadsFor(Seq.empty) mustBe Seq.empty
    }

    "map a thread to a dashboard thread" in {
      val thread = Thread(
        threadReference = ThreadReference("THREAD001"),
        relatedReference = Some("CASE001"),
        externalContact = "Jane Smith",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(LocalDate.of(2026, 8, 30))
      )

      dashboardThreadsFor(Seq(thread)) mustBe Seq(
        DashboardThread(
          threadReference = ThreadReference("THREAD001"),
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
        threadReference = ThreadReference("THREAD002"),
        relatedReference = None,
        externalContact = "John Smith",
        status = "Open",
        waitingOn = "Recipient",
        deadline = None
      )

      dashboardThreadsFor(Seq(thread)) mustBe Seq(
        DashboardThread(
          threadReference = ThreadReference("THREAD002"),
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
        threadReference = ThreadReference("THREAD003"),
        relatedReference = Some("CASE003"),
        externalContact = "Alex Brown",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(LocalDate.of(2026, 1, 5))
      )

      dashboardThreadsFor(Seq(thread)).head.deadline mustBe "05/01/2026"
    }

    "set priority to Overdue when the deadline is before today" in {
      val thread = Thread(
        threadReference = ThreadReference("THREAD004"),
        relatedReference = Some("CASE004"),
        externalContact = "Alex Brown",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(today.minusDays(1))
      )

      dashboardThreadsFor(Seq(thread)).head.priority mustBe ThreadPriority.Overdue
    }

    "set priority to ResponseReceived when the status is Needs action" in {
      val thread = Thread(
        threadReference = ThreadReference("THREAD005"),
        relatedReference = Some("CASE005"),
        externalContact = "Alex Brown",
        status = "Needs action",
        waitingOn = "HMRC",
        deadline = Some(today.plusDays(1))
      )

      dashboardThreadsFor(Seq(thread)).head.priority mustBe
        ThreadPriority.ResponseReceived
    }

    "prioritise an overdue thread as Overdue even when its status is Needs action" in {
      val thread = Thread(
        threadReference = ThreadReference("THREAD006"),
        relatedReference = Some("CASE006"),
        externalContact = "Alex Brown",
        status = "Needs action",
        waitingOn = "HMRC",
        deadline = Some(today.minusDays(1))
      )

      dashboardThreadsFor(Seq(thread)).head.priority mustBe ThreadPriority.Overdue
    }

    "set priority to None when the deadline is today" in {
      val thread = Thread(
        threadReference = ThreadReference("THREAD007"),
        relatedReference = Some("CASE007"),
        externalContact = "Alex Brown",
        status = "Open",
        waitingOn = "HMRC",
        deadline = Some(today)
      )

      dashboardThreadsFor(Seq(thread)).head.priority mustBe ThreadPriority.None
    }
  }

  private def dashboardThreadsFor(threads: Seq[Thread]): Seq[DashboardThread] = {
    when(mockThreadConnector.getAll()(using any[HeaderCarrier]))
      .thenReturn(Future.successful(threads))

    service.getDashboardThreads().futureValue
  }
}
