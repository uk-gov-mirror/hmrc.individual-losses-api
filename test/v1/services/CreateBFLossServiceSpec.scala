/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.services

import uk.gov.hmrc.domain.Nino
import v1.mocks.connectors.MockBFLossConnector
import v1.models.des.CreateBFLossResponse
import v1.models.domain.{BFLoss, TypeOfLoss}
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.CreateBFLossRequest

import scala.concurrent.Future

class CreateBFLossServiceSpec extends ServiceSpec {

  val nino: Nino = Nino("AA123456A")
  val lossId = "AAZZ1234567890a"

  val bfLoss: BFLoss = BFLoss(TypeOfLoss.`self-employment`, Some("XKIS00000000988"), "2019-20", 256.78)

  val serviceUnavailableError: MtdError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockBFLossConnector {
    lazy val service = new CreateBFLossService(connector)
  }

  "create BFLoss" when {
    lazy val request = CreateBFLossRequest(nino, bfLoss)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedBFLossConnector
          .createBFLoss(request)
          .returns(Future.successful(Right(DesResponse(correlationId, CreateBFLossResponse(lossId)))))

        await(service.createBFLoss(request)) shouldBe Right(DesResponse(correlationId, CreateBFLossResponse(lossId)))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError = MtdError("SOME_CODE", "some message")
        val desResponse: DesResponse[OutboundError] = DesResponse(correlationId, OutboundError(someError))
        MockedBFLossConnector.createBFLoss(request).returns(Future.successful(Left(desResponse)))

        await(service.createBFLoss(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedBFLossConnector.createBFLoss(request).returns(Future.successful(Left(expected)))
        val result: CreateBFLossOutcome = await(service.createBFLoss(request))
        result shouldBe Left(ErrorWrapper(correlationId, DownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID"  -> NinoFormatError,
      "DUPLICATE"                  -> RuleDuplicateSubmissionError,
      "NOT_FOUND_INCOME_SOURCE"    -> NotFoundError,
      "TAX_YEAR_NOT_SUPPORTED"     -> RuleTaxYearNotSupportedError,
      "TAX_YEAR_NOT_ENDED"         -> RuleTaxYearNotEndedError,
      "INVALID_PAYLOAD"            -> DownstreamError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError,
      "UNEXPECTED_ERROR"           -> DownstreamError,
      "INVALID_TAX_YEAR"          -> DownstreamError,
      "INCOME_SOURCE_NOT_ACTIVE"  -> DownstreamError,
      "ACCOUNTING_PERIOD_NOT_ENDED" -> DownstreamError,
      "INVALID_CLAIM_TYPE"        -> DownstreamError,
      "NO_ACTIVE_ACCOUNTING_PERIOD" -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedBFLossConnector
              .createBFLoss(request)
              .returns(Future.successful(Left(DesResponse(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.createBFLoss(request)) shouldBe Left(ErrorWrapper(correlationId, v, None))
          }
        }
    }
  }
}
