package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the prisoner-search-api.
 */
class PrisonerSearchApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearchApi = PrisonerSearchApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearchApi.start()
    prisonerSearchApi.stubImprisonedPrisoner()
    prisonerSearchApi.stubBailPrisoner()
    prisonerSearchApi.stubRelatedOffencesPrisoner()
    prisonerSearchApi.stubMultipleOffences()
    prisonerSearchApi.stubIntersectingSentence()
    prisonerSearchApi.stubCrdValidation()
    prisonerSearchApi.stubActiveBookingHasNoOffenceDates()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearchApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearchApi.stop()
  }
}

class PrisonerSearchApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8335
  }

  fun stubImprisonedPrisoner() {
    stubFor(
      get("/prisoner-search-api/prisoner/${PrisonApiExtension.IMPRISONED_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "prisonerNumber": "${PrisonApiExtension.IMPRISONED_PRISONER}",
                  "prisonId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubBailPrisoner() {
    stubFor(
      get("/prisoner-search-api/prisoner/${PrisonApiExtension.BAIL_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "prisonerNumber": "${PrisonApiExtension.BAIL_PRISONER}",
                  "prisonId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubIntersectingSentence() {
    stubFor(
      get("/prisoner-search-api/prisoner/${PrisonApiExtension.INTERSECTING_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "prisonerNumber": "${PrisonApiExtension.INTERSECTING_PRISONER}",
                  "prisonId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubCrdValidation() {
    stubFor(
      get("/prisoner-search-api/prisoner/${PrisonApiExtension.CRD_VALIDATION_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "prisonerNumber": "${PrisonApiExtension.CRD_VALIDATION_PRISONER}",
                  "prisonId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubRelatedOffencesPrisoner() {
    stubFor(
      get("/prisoner-search-api/prisoner/${PrisonApiExtension.RELATED_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "prisonerNumber": "${PrisonApiExtension.BAIL_PRISONER}",
                  "prisonId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubMultipleOffences() {
    stubFor(
      get("/prisoner-search-api/prisoner/${PrisonApiExtension.MULTIPLE_OFFENCES_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 2,
                  "prisonerNumber": "${PrisonApiExtension.MULTIPLE_OFFENCES_PRISONER}",
                  "prisonId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubActiveBookingHasNoOffenceDates() {
    stubFor(
      get("/prisoner-search-api/prisoner/${PrisonApiExtension.NO_OFFENCE_DATES}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "prisonerNumber": "${PrisonApiExtension.NO_OFFENCE_DATES}",
                  "prisonId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
