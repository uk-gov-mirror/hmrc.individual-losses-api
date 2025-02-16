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

package v2.mocks

import config.AppConfig
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory

trait MockAppConfig extends MockFactory {

  val mockAppConfig: AppConfig = mock[AppConfig]

  object MockedAppConfig {
    def desBaseUrl: CallHandler[String] = (mockAppConfig.desBaseUrl _: () => String).expects()
    def desToken: CallHandler[String] = (mockAppConfig.desToken _).expects()
    def desEnvironment: CallHandler[String] = (mockAppConfig.desEnv _).expects()
    def mtdIdBaseUrl: CallHandler[String] = (mockAppConfig.mtdIdBaseUrl _: () => String).expects()
  }
}
