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
import v1.mocks.validators.MockCreateLossClaimValidator
import v1.models.domain.{LossClaim, TypeOfClaim, TypeOfLoss}
import v1.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import v1.models.requestData._

class CreateLossClaimParserSpec extends UnitSpec {
  val nino = "AA123456B"
  val taxYear = "2019-20"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val requestBodyJson = Json.parse(
    s"""{
       |  "typeOfLoss" : "self-employment",
       |  "selfEmploymentId" : "XAIS01234567890",
       |  "taxYear" : "$taxYear",
       |  "typeOfClaim" : "carry-forward"
       |}""".stripMargin)

  val inputData: CreateLossClaimRawData =
    CreateLossClaimRawData(nino, AnyContentAsJson(requestBodyJson))

  trait Test extends MockCreateLossClaimValidator {
    lazy val parser = new CreateLossClaimParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockValidator.validate(inputData).returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(CreateLossClaimRequest(Nino(nino), LossClaim("2019-20", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, Some("XAIS01234567890"))))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockValidator.validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockValidator.validate(inputData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }
}
