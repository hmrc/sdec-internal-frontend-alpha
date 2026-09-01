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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.PathBindable

class ThreadReferenceSpec extends AnyFreeSpec with Matchers {

  private val testBindable = summon[PathBindable[ThreadReference]]

  private val validThreadReference: ThreadReference =
    ThreadReference.from("ABCDEF123456").getOrElse(throw new Exception("Invalid test Thread Reference"))

  "from" - {
    "must return a valid ThreadReference for a 12 character uppercase alphanumeric string" in {
      ThreadReference.from("ABCDEF123456").map(_.value) mustBe Right("ABCDEF123456")
    }

    "must return Empty when the value is an empty string" in {
      ThreadReference.from("").map(_.value) mustBe Left(ThreadReferenceError.Empty)
    }

    "must return InvalidLength when the value is not 12 characters long" in {
      ThreadReference.from("ABC1234").map(_.value) mustBe Left(ThreadReferenceError.InvalidLength)
    }

    "must return InvalidFormat when the value contains lowercase characters" in {
      ThreadReference.from("ABcdef123456").map(_.value) mustBe Left(ThreadReferenceError.InvalidFormat("ABcdef123456"))
    }

    "must return InvalidFormat when the value contains special characters" in {
      ThreadReference.from("ABCDE+12345!").map(_.value) mustBe Left(ThreadReferenceError.InvalidFormat("ABCDE+12345!"))
    }
  }

  "Path Bindable" - {
    "must bind a valid path segment" in {
      testBindable.bind("threadReference", "ABCDEF123456").map(_.value) mustBe Right("ABCDEF123456")
    }

    "must fail to bind an invalid path segment" in {
      testBindable.bind("threadReference", "ABCDE+12345!").map(_.value) mustBe Left("Invalid Thread Reference")
    }

    "must unbind back to the raw value" in {
      testBindable.unbind("threadReference", validThreadReference) mustBe "ABCDEF123456"
    }
  }

}
