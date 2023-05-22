package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the calculate release dates api.
 */
class CalculateReleaseDatesApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val calculateReleaseDatesApi = CalculateReleaseDatesApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    calculateReleaseDatesApi.start()
    calculateReleaseDatesApi.stubIntersectingSentence()
    calculateReleaseDatesApi.stubCrdValidation()
  }

  override fun beforeEach(context: ExtensionContext) {
    calculateReleaseDatesApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    calculateReleaseDatesApi.stop()
  }
}

class CalculateReleaseDatesApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8333
  }

  fun stubIntersectingSentence() {
    stubFor(
      post("/calculate-release-dates-api/calculation/relevant-remand/${PrisonApiExtension.INTERSECTING_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
             { "releaseDate": "2021-04-01" }
               
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubCrdValidation() {
    stubFor(
      post("/calculate-release-dates-api/calculation/relevant-remand/${PrisonApiExtension.CRD_VALIDATION_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {"releaseDate":null,"validationMessages":[{"message":"Unsupported sentence type 2020 Uknown"}]}
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
