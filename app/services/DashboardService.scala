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

import connectors.{TeamConnector, ThreadConnector}
import models.{Thread, ThreadFilter, ThreadQuery}
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{Dashboard, DashboardThread, ThreadPriority}

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DashboardService @Inject() (
  threadSummaryConnector: ThreadConnector,
  teamConnector:          TeamConnector,
  clock:                  Clock
)(using ExecutionContext) {

  private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def getDashboard(userId: String, selectedFilter: Option[ThreadFilter])(using HeaderCarrier): Future[Dashboard] =
    for {
      teams <- teamConnector.findTeamsByPid(userId)
      availableFilters = ThreadFilter.availableFor(teams)
      appliedFilter    = selectedFilter.filter(availableFilters.contains)
      threads <- getDashboardThreads(userId, appliedFilter)
    } yield Dashboard(threads, availableFilters, appliedFilter)

  private def getDashboardThreads(userId: String, selectedFilter: Option[ThreadFilter] = None)(using
    HeaderCarrier
  ): Future[Seq[DashboardThread]] =
    threadSummaryConnector
      .getAll(filterToThreadQuery(userId, selectedFilter))
      .map(_.map(toDashboardThread))

  private def filterToThreadQuery(userId: String, threadFilter: Option[ThreadFilter]): ThreadQuery =
    threadFilter match {
      case Some(ThreadFilter.MyThreads)     => ThreadQuery(threadOwner = Some(userId))
      case Some(ThreadFilter.NeedsAction)   => ThreadQuery()
      case Some(ThreadFilter.Waiting)       => ThreadQuery()
      case Some(ThreadFilter.Overdue)       => ThreadQuery()
      case Some(ThreadFilter.InProgress)    => ThreadQuery()
      case Some(ThreadFilter.OpenThreads)   => ThreadQuery()
      case Some(ThreadFilter.ClosedThreads) => ThreadQuery()
      case None                             => ThreadQuery()
    }

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
    if thread.deadline.exists(_.isBefore(LocalDate.now(clock))) then ThreadPriority.Overdue
    else if thread.status == "Needs action" then ThreadPriority.ResponseReceived
    else ThreadPriority.None

}
