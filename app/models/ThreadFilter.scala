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

import play.api.mvc.QueryStringBindable

enum ThreadFilter(val value: String) {
  case MyThreads extends ThreadFilter(value = "my-threads")
  case NeedsAction extends ThreadFilter(value = "needs-action")
  case Waiting extends ThreadFilter(value = "waiting")
  case Overdue extends ThreadFilter(value = "overdue")
  case InProgress extends ThreadFilter(value = "in-progress")
  case OpenThreads extends ThreadFilter(value = "open-threads")
  case ClosedThreads extends ThreadFilter(value = "closed-threads")
}

object ThreadFilter {

  extension (value: String) {
    def toThreadFilterOpt: Option[ThreadFilter] = values.find(_.value == value)
  }

  given queryStringBindable(using stringBinder: QueryStringBindable[String]): QueryStringBindable[ThreadFilter] with {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ThreadFilter]] =
      stringBinder.bind(key, params).flatMap {
        case Right(value) => value.toThreadFilterOpt.map(Right(_))
        case Left(error)  => Some(Left(error))
      }

    override def unbind(key: String, threadFilter: ThreadFilter): String =
      stringBinder.unbind(key, threadFilter.value)
  }

}
