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

package models

import base.SpecBase

class ThreadQuerySpec extends SpecBase {

  private val userId: String = "pid-cb-001"

  "toQueryParameters" - {
    "must be empty when no criteria are set" in {
      ThreadQuery().toQueryParameters mustBe Seq.empty
    }

    "must set the thread owner as the 'threadOwner' parameter" in {
      ThreadQuery(threadOwner = Some(userId)).toQueryParameters mustBe Seq("threadOwner" -> userId)
    }
  }
}
