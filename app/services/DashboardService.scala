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

import models.Thread
import viewmodels.{DashboardThread, ThreadPriority}

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}

@Singleton
class DashboardService @Inject() (clock: Clock):

    private val dateFormatter: DateTimeFormatter =
      DateTimeFormatter.ofPattern("dd/MM/yyyy")

    def buildThreads(threads: Seq[Thread]): Seq[DashboardThread] =
      threads.map(toDashboardThread)

    private def toDashboardThread(thread: Thread): DashboardThread =
      DashboardThread(
        threadReference = thread.threadReference,
        relatedReference = thread.relatedReference.getOrElse("-"),
        externalContact = thread.externalContact,
        status = thread.status,
        waitingOn = thread.waitingOn,
        deadline = thread.deadline
          .map(_.format(dateFormatter))
          .getOrElse("-"),
        priority = determinePriority(thread)
      )

    private def determinePriority(thread: Thread): ThreadPriority =
      if thread.deadline.exists(_.isBefore(LocalDate.now(clock))) then
          ThreadPriority.Overdue
      else if thread.status == "Needs action" then ThreadPriority.ResponseReceived
      else ThreadPriority.None
