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

package v1.controllers.requestParsers

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v1.mocks.validators.MockAmendLossClaimsOrderValidator
import v1.models.domain.{AmendLossClaimsOrderRequestBody, Claim, TypeOfClaim}
import v1.models.errors.{BadRequestError, ClaimIdFormatError, ErrorWrapper, NinoFormatError}
import v1.models.requestData.{AmendLossClaimsOrderRawData, AmendLossClaimsOrderRequest, DesTaxYear}

class AmendLossClaimsOrderParserSpec extends UnitSpec {

  private val nino = "AA123456A"
  private val claimType = "carry-sideways"
  private val taxYear = "2020-21"
  private val claim = Json.obj("id" -> "1234568790ABCDE", "sequence" -> 1)
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val data: AmendLossClaimsOrderRawData = AmendLossClaimsOrderRawData(nino, Some(taxYear), AnyContentAsJson(Json.obj("claimType" -> claimType, "listOfLossClaims" -> Seq(claim))))

  val dataNoTaxYear = AmendLossClaimsOrderRawData(nino, None, AnyContentAsJson(Json.obj("claimType" -> claimType, "listOfLossClaims" -> Seq(claim))))


  trait Test extends MockAmendLossClaimsOrderValidator {
    lazy val parser = new AmendLossClaimsOrderParser(mockValidator)
  }

  "parse" should {
    "return an AmendLossClaimsOrderRequest" when {
      "the validator returns no errors and a tax year is supplied" in new Test {
        MockValidator.validate(data).returns(List())

        parser.parseRequest(data) shouldBe {
          Right(AmendLossClaimsOrderRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, Seq(Claim("1234568790ABCDE", 1)))))
        }
      }
      "the validator returns no errors and no tax year is supplied" in new Test {
        MockValidator.validate(dataNoTaxYear).returns(List())
        parser.parseRequest(dataNoTaxYear) shouldBe {
          Right(AmendLossClaimsOrderRequest(Nino(nino), DesTaxYear.mostRecentTaxYear(), AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, Seq(Claim("1234568790ABCDE", 1)))))
        }
      }
    }
    "return a single error" when {
      "the validator returns a single error" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }
    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError, ClaimIdFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, ClaimIdFormatError))))
      }
    }
  }


}
