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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host:    String = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost                  = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "sdec-internal-frontend"

  def feedbackUrl(using request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl:         String = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl:       String = configuration.get[String]("urls.signOut")

  private val exitSurveyBaseUrl: String = servicesConfig.baseUrl(serviceName = "feedback-frontend")

  val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/sdec-internal-frontend"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  private val threadInfoApiBaseUrl: String = servicesConfig.baseUrl(serviceName = "sdec-threadinfo-api-alpha")

  val workspaceAccessUrl: String =
    s"$threadInfoApiBaseUrl/sdec-threadinfo-api-alpha/workspace"

  val threadSummariesUrl: String = s"$threadInfoApiBaseUrl/sdec-threadinfo-api-alpha/threads"
  val threadReferenceUrl: String = s"$threadInfoApiBaseUrl/sdec-threadinfo-api-alpha/thread-reference"
  val threadCreateUrl:    String = s"$threadInfoApiBaseUrl/sdec-threadinfo-api-alpha/thread-create"
  val staffUrl:           String = s"$threadInfoApiBaseUrl/sdec-threadinfo-api-alpha/staff"

  val timeout:   Int = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")
}
