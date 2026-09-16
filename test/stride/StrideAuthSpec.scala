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

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction}
import models.requests.*
import org.scalatest.freespec.AnyFreeSpec
import play.api.Application
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import stride.{StrideAuth, StrideAuthUser}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class StrideAuthSpec extends SpecBase {
  given ExecutionContext = ExecutionContext.global

  private val application: Application =
    applicationBuilder().build()

  private val config: FrontendAppConfig =
    application.injector.instanceOf[FrontendAppConfig]

  private val actionBuilder: DefaultActionBuilder =
    application.injector.instanceOf[DefaultActionBuilder]

  private val authConnector: AuthConnector =
    application.injector.instanceOf[AuthConnector]

  private val testUser =
    StrideAuthUser(
      credentials = Credentials(
        providerId = "test-provider-id",
        providerType = "PrivilegedApplication"
      ),
      email = "test@example.com",
      authorisedEnrollments = Enrolments(Set()),
      allEnrollments = Enrolments(Set()),
      name = Name(Some("Test User"), None)
    )

  private val request: Request[AnyContent] =
    FakeRequest(GET, "/test")

  "authorisedFromStride" - {

    "must call the supplied action when authentication succeeds" in {

      val strideAuth =
        testStrideAuth(Future.successful(testUser))

      val result =
        strideAuth
          .authorisedFromStride { (user, actualRequest) =>
            user mustBe testUser
            actualRequest mustBe request

            Future.successful(Ok)
          }
          .apply(request)

      status(result) mustBe OK
    }

    "must redirect to login when there is no active session" in {

      val strideAuth =
        testStrideAuth(
          Future.failed(
            new NoActiveSession("No active session") {}
          )
        )

      val result =
        strideAuth
          .authorisedFromStride { (_, _) =>
            fail("The action must not be called")
          }
          .apply(request)

      status(result) mustBe SEE_OTHER
    }

    "must redirect to insufficient roles when enrolments are insufficient" in {

      val strideAuth =
        testStrideAuth(
          Future.failed(
            new InsufficientEnrolments("Insufficient enrolments")
          )
        )

      val result =
        strideAuth
          .authorisedFromStride { (_, _) =>
            fail("The action must not be called")
          }
          .apply(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(
        controllers.routes.InsufficientRolesController.get.url
      )
    }

    "must return the result from the supplied action" in {

      val strideAuth =
        testStrideAuth(Future.successful(testUser))

      val result =
        strideAuth
          .authorisedFromStride { (_, _) =>
            Future.successful(Ok("success"))
          }
          .apply(request)

      status(result) mustBe OK
      contentAsString(result) mustBe "success"
    }
  }

  "authorisedFromStrideWithData" - {

    "must call the supplied action when data is available" in {

      val userAnswers =
        emptyUserAnswers

      val optionalDataRequest =
        OptionalDataRequest(
          request = request,
          userId = testUser.credentials.providerId,
          userAnswers = Some(userAnswers)
        )

      val dataRequest =
        DataRequest(
          request = request,
          userId = testUser.credentials.providerId,
          userAnswers = userAnswers
        )

      val dataRetrievalAction =
        new StubDataRetrievalAction(
          Future.successful(
            optionalDataRequest
          )
        )

      val dataRequiredAction =
        new StubDataRequiredAction(
          Future.successful(Right(dataRequest))
        )

      val strideAuth =
        testStrideAuth(
          authenticationResult = Future.successful(testUser),
          dataRetrievalAction = dataRetrievalAction,
          dataRequiredAction = dataRequiredAction
        )

      val result =
        strideAuth
          .authorisedFromStrideWithData { (user, actualDataRequest) =>
            user mustBe testUser
            actualDataRequest mustBe dataRequest

            Future.successful(Ok("success"))
          }
          .apply(request)

      status(result) mustBe OK
      contentAsString(result) mustBe "success"
    }

    "must return the result from DataRequiredAction when data is missing" in {

      val expectedResult =
        Redirect("/missing-data")

      val dataRetrievalAction =
        new StubDataRetrievalAction(
          Future.successful(
            OptionalDataRequest(
              request = request,
              userId = testUser.credentials.providerId,
              userAnswers = None
            )
          )
        )

      val dataRequiredAction =
        new StubDataRequiredAction(
          Future.successful(Left(expectedResult))
        )

      val strideAuth =
        testStrideAuth(
          authenticationResult = Future.successful(testUser),
          dataRetrievalAction = dataRetrievalAction,
          dataRequiredAction = dataRequiredAction
        )

      val result =
        strideAuth
          .authorisedFromStrideWithData { (_, _) =>
            fail("The action must not be called")
          }
          .apply(request)
          .futureValue

      result mustBe expectedResult
    }

    "must propagate a failure from DataRetrievalAction" in {
      val expectedException =
        new RuntimeException("data retrieval failed")

      val dataRetrievalAction =
        new StubDataRetrievalAction(
          Future.failed(expectedException)
        )

      val strideAuth =
        testStrideAuth(
          authenticationResult = Future.successful(testUser),
          dataRetrievalAction = dataRetrievalAction
        )

      val result =
        strideAuth
          .authorisedFromStrideWithData { (_, _) =>
            Future.successful(Ok)
          }
          .apply(request)
          .failed
          .futureValue

      result mustBe expectedException
    }

    "must propagate a failure from DataRequiredAction" in {

      val expectedException =
        new RuntimeException("data required failed")

      val dataRequiredAction =
        new StubDataRequiredAction(
          Future.failed(expectedException)
        )

      val strideAuth =
        testStrideAuth(
          authenticationResult = Future.successful(testUser),
          dataRequiredAction = dataRequiredAction
        )

      val result =
        strideAuth
          .authorisedFromStrideWithData { (_, _) =>
            Future.successful(Ok)
          }
          .apply(request)
          .failed
          .futureValue

      result mustBe expectedException
    }
  }

  private def testStrideAuth(
    authenticationResult: Future[StrideAuthUser],
    dataRetrievalAction:  DataRetrievalAction = application.injector.instanceOf[DataRetrievalAction],
    dataRequiredAction:   DataRequiredAction = application.injector.instanceOf[DataRequiredAction]
  ): StrideAuth =
    new StrideAuth(
      authConnector = authConnector,
      actionBuilder = actionBuilder,
      config = config,
      dataRetrievalAction = dataRetrievalAction,
      dataRequiredAction = dataRequiredAction
    ) {

      override protected def authenticate(
        request: Request[AnyContent]
      )(using
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[StrideAuthUser] =
        authenticationResult
    }

  private class StubDataRetrievalAction(
    result: Future[OptionalDataRequest[AnyContent]]
  )(using ec: ExecutionContext)
      extends DataRetrievalAction {

    override protected def executionContext: ExecutionContext =
      ec

    override protected def transform[A](
      request: IdentifierRequest[A]
    ): Future[OptionalDataRequest[A]] =
      result.map { optionalDataRequest =>
        OptionalDataRequest(
          request = request,
          userId = optionalDataRequest.userId,
          userAnswers = optionalDataRequest.userAnswers
        )
      }
  }

  private final class StubDataRequiredAction(
    result: Future[Either[Result, DataRequest[AnyContent]]]
  )(using ec: ExecutionContext)
      extends DataRequiredAction {

    override protected def executionContext: ExecutionContext = ec

    override protected def refine[A](
      request: OptionalDataRequest[A]
    ): Future[Either[Result, DataRequest[A]]] =
      result.map {
        case Left(result) =>
          Left(result)

        case Right(dataRequest) =>
          Right(
            DataRequest(
              request = request,
              userId = dataRequest.userId,
              userAnswers = dataRequest.userAnswers
            )
          )
      }
  }
}
