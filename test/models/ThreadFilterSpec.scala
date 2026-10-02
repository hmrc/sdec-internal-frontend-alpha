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
import models.ThreadFilter.toThreadFilterOpt
import play.api.mvc.QueryStringBindable

class ThreadFilterSpec extends SpecBase {

  private val testBindable: QueryStringBindable[ThreadFilter] = summon[QueryStringBindable[ThreadFilter]]

  private val filterKey:          String = "filter"
  private val invalidFilterValue: String = "invalid-filter-value"

  "toThreadFilterOpt" - {
    "must find every thread filter by its value" in
      ThreadFilter.values.foreach { threadFilter =>
        threadFilter.value.toThreadFilterOpt mustBe Some(threadFilter)
      }

    "must return None for unknown value" in {
      invalidFilterValue.toThreadFilterOpt mustBe None
    }
  }

  "QueryStringBindable" - {
    "must bind the 'filter=my-threads' URL parameter to the MyThreads ThreadFilter" in {
      testBindable.bind(filterKey, Map(filterKey -> Seq("my-threads"))) mustBe Some(Right(ThreadFilter.MyThreads))
    }

    "must ignore URL query parameter key other than 'filter'" in {
      testBindable.bind(filterKey, Map("status" -> Seq("my-threads"))) mustBe None
    }

    "must ignore an unknown value for the 'filter' key" in {
      testBindable.bind(filterKey, Map(filterKey -> Seq(invalidFilterValue))) mustBe None
    }

    "must return None when the key is missing" in {
      testBindable.bind(filterKey, Map.empty) mustBe None
    }

    "must unbind a filter to key=value" in {
      testBindable.unbind(filterKey, ThreadFilter.MyThreads) mustBe "filter=my-threads"
    }
  }

}
