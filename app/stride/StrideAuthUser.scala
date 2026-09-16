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

package stride

import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}

case class StrideAuthUser(
  credentials:           Credentials,
  email:                 String,
  authorisedEnrollments: Enrolments,
  allEnrollments:        Enrolments,
  name:                  Name
)

object StrideAuthUser {

  def apply(
    credentialOptions:     Option[Credentials],
    email:                 Option[String],
    authorisedEnrollments: Enrolments,
    allEnrollments:        Enrolments,
    nameOption:            Option[Name]
  ): StrideAuthUser =
    StrideAuthUser(
      credentials = credentialOptions.getOrElse(
        throw new IllegalArgumentException("No STRIDE credentials provided")
      ),
      email = email.getOrElse(
        throw new IllegalArgumentException("No STRIDE Email provided")
      ),
      authorisedEnrollments = authorisedEnrollments,
      allEnrollments = allEnrollments,
      name = nameOption.getOrElse(Name(None, None))
    )
}
