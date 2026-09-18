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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction}
import models.requests.DataRequest
import models.requests.IdentifierRequest.identifierRequest
import play.api.mvc.*

import scala.concurrent.{ExecutionContext, Future}

class TestStrideAuth @Inject() (
  strideUser:          StrideAuthUser,
  dataRetrievalAction: DataRetrievalAction,
  dataRequiredAction:  DataRequiredAction,
  actionBuilder:       DefaultActionBuilder
) extends StrideAuthAlgebra {

  override def authorisedFromStride(
    action: (StrideAuthUser, Request[AnyContent]) => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] =
    actionBuilder.async { request =>
      action(strideUser, request)
    }

  override def authorisedFromStrideWithData(
    action: (StrideAuthUser, DataRequest[AnyContent]) => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] =
    authorisedFromStride { (user, request) =>
      val idRequest = identifierRequest(user, request)
      for {
        optionalDataRequest <- dataRetrievalAction.retrieve(idRequest)
        refinedRequest      <- dataRequiredAction.requireData(optionalDataRequest)
        result              <- refinedRequest match {
                    case Left(result)       => Future.successful(result)
                    case Right(dataRequest) => action(user, dataRequest)
                  }
      } yield result
    }
}
