/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.models.des

import play.api.libs.json._
import v1.models.domain.TypeOfLoss

sealed trait IncomeSourceType {
  def toTypeOfLoss: TypeOfLoss
}

object IncomeSourceType {

  case object `02` extends IncomeSourceType {
    override def toTypeOfLoss: TypeOfLoss = TypeOfLoss.`uk-property-non-fhl`
  }
  case object `04` extends IncomeSourceType {
    override def toTypeOfLoss: TypeOfLoss = TypeOfLoss.`uk-property-fhl`
  }

  implicit val reads: Reads[IncomeSourceType] = implicitly[Reads[String]].collect(JsonValidationError("error.expected.incomeSourceType")) {
    case "02" => `02`
    case "04" => `04`
  }

  implicit val writes: Writes[IncomeSourceType] = Writes[IncomeSourceType](ts => JsString(ts.toString))
}
