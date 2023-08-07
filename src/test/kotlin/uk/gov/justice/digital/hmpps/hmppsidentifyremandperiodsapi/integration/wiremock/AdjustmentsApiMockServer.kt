package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the adjustments api.
 */
class AdjustmentsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val adjustmentsApi = AdjustmentsApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    adjustmentsApi.start()
    adjustmentsApi.stubCreate()
    adjustmentsApi.stubGet()
  }

  override fun beforeEach(context: ExtensionContext) {
    adjustmentsApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    adjustmentsApi.stop()
  }
}

class AdjustmentsApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8334
  }

  fun stubGet() {
    stubFor(
      get("/adjustments-api/adjustments?person=${PrisonApiExtension.IMPRISONED_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              []
              """)
        )
    )
  }

  fun stubCreate() {
    stubFor(
      post("/adjustments-api/adjustments")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{}""".trimIndent(),
            )
            .withStatus(200),
        )
    )
  }
}