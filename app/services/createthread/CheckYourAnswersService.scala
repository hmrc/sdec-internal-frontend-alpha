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

package services.createthread

import models.{RecipientDetails, ThreadDetails}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.DateTimeFormats.toDateFormat
import viewmodels.govuk.summarylist.*

import javax.inject.Singleton

@Singleton
class CheckYourAnswersService:

  def recipientDetailsList(recipient: RecipientDetails)(using messages: Messages): SummaryList =
    val changeHref = controllers.createthread.routes.RecipientDetailsController.onPageLoad().url

    val rows = Seq(
      row("checkYourAnswers.recipientDetails.name", s"${recipient.firstName} ${recipient.lastName}", changeHref),
      row("checkYourAnswers.recipientDetails.email", recipient.email, changeHref),
      row(
        "checkYourAnswers.recipientDetails.phoneNumber",
        recipient.phoneNumber,
        changeHref
      ),
      row(
        "checkYourAnswers.recipientDetails.nationalInsuranceNumber",
        recipient.nationalInsuranceNumber,
        changeHref
      ),
      row(
        "checkYourAnswers.recipientDetails.hasRelatedCase",
        if recipient.hasRelatedCase then messages("site.yes") else messages("site.no"),
        changeHref
      )
    ) ++ recipient.caseReferenceNumber.toSeq.map { caseReferenceNumber =>
      row(
        "checkYourAnswers.recipientDetails.caseReferenceNumber",
        caseReferenceNumber,
        changeHref
      )
    }

    SummaryListViewModel(rows)

  def threadDetailsList(threadDetails: ThreadDetails)(using Messages): SummaryList =
    val changeHref = controllers.createthread.routes.ThreadDetailsController.onPageLoad().url

    SummaryListViewModel(
      Seq(
        row("checkYourAnswers.threadDetails.message", threadDetails.message, changeHref),
        row("checkYourAnswers.threadDetails.responseDate", threadDetails.responseDate.toDateFormat, changeHref)
      )
    )

  private def row(labelKey: String, value: String, changeHref: String)(using messages: Messages) =
    SummaryListRowViewModel(
      key = KeyViewModel(Text(messages(labelKey))),
      value = ValueViewModel(Text(value)),
      actions = Seq(
        ActionItemViewModel(Text(messages("site.change")), changeHref)
          .withVisuallyHiddenText(messages(s"$labelKey.hidden"))
      )
    )
