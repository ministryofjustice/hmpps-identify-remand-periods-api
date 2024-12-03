package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.controller

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock.AdjustmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeLegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.GenericLegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.IdentifyRemandDecisionDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Offence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.repository.IdentifyRemandDecisionRepository
import java.time.LocalDate

class RelevantRemandControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var decisionRepository: IdentifyRemandDecisionRepository

  @Test
  fun `Run calculation for a imprisoned prisoner`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.IMPRISONED_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .bodyValue(RemandCalculationRequestOptions(true))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.adjustments).isNotEmpty
    assertThat(result.adjustments[0].fromDate).isEqualTo(LocalDate.of(2022, 10, 13))
    assertThat(result.adjustments[0].toDate).isEqualTo(LocalDate.of(2022, 12, 12))
    assertThat(result.issuesWithLegacyData).isEqualTo(
      listOf(
        ChargeLegacyDataProblem(LegacyDataProblemType.MISSING_COURT_OUTCOME, message = "The court hearing on 13 Dec 2022 for 'An offence' has a missing hearing outcome within booking ABC123.", offence = Offence(code = "SX03163A", statute = "SX03", description = "An offence"), bookingId = 1, bookNumber = "ABC123", courtCaseRef = null),
        GenericLegacyDataProblem(LegacyDataProblemType.MISSING_COURT_EVENT_FOR_IMPRISONMENT_STATUS_RECALL, "The offenders main inmate status was changed to recalled on 10 Jan 2024 but there is no matching court events"),
      ),
    )
    assertThat(result.remandCalculation).isNotNull
  }

  @Test
  fun `Run calculation for a bail prisoner`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.BAIL_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.adjustments).isNotEmpty
    assertThat(result.adjustments[0].fromDate).isEqualTo(LocalDate.of(2015, 3, 25))
    assertThat(result.adjustments[0].toDate).isEqualTo(LocalDate.of(2015, 4, 8))
  }

  @Test
  fun `Run calculation for a prisoner with related offences`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.RELATED_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.adjustments).isNotEmpty
    assertThat(result.adjustments[0].fromDate).isEqualTo(LocalDate.of(2015, 3, 20))
    assertThat(result.adjustments[0].toDate).isEqualTo(LocalDate.of(2015, 4, 10))
  }

  @Test
  fun `Run calculation for a prisoner with multiple offences`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.MULTIPLE_OFFENCES_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.chargeRemand).isNotEmpty
    assertThat(result.chargeRemand.size).isEqualTo(4)

    assertThat(result.chargeRemand[0].from).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.chargeRemand[0].to).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.chargeRemand[0].fromEvent).isEqualTo(CourtAppearance(LocalDate.of(2019, 7, 6), "Commit/Transfer/Send to Crown Court for Trial in Custody"))
    assertThat(result.chargeRemand[0].toEvent).isEqualTo(CourtAppearance(LocalDate.of(2020, 9, 24), "Commit to Crown Court for Sentence Conditional Bail"))
    assertThat(result.chargeRemand[0].chargeIds).isEqualTo(listOf(3L))

    assertThat(result.chargeRemand[1].from).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.chargeRemand[1].to).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.chargeRemand[1].fromEvent).isEqualTo(CourtAppearance(LocalDate.of(2019, 7, 6), "Commit to Crown Court for Trial (Summary / Either Way Offences)"))
    assertThat(result.chargeRemand[1].toEvent).isEqualTo(CourtAppearance(LocalDate.of(2020, 9, 24), "Commit to Crown Court for Sentence Conditional Bail"))
    assertThat(result.chargeRemand[1].chargeIds).isEqualTo(listOf(4L))

    assertThat(result.chargeRemand[2].from).isEqualTo(LocalDate.of(2021, 6, 14))
    assertThat(result.chargeRemand[2].to).isEqualTo(LocalDate.of(2021, 6, 14))
    assertThat(result.chargeRemand[2].fromEvent).isEqualTo(CourtAppearance(LocalDate.of(2021, 6, 14), "Sentence Postponed"))
    assertThat(result.chargeRemand[2].toEvent).isEqualTo(CourtAppearance(LocalDate.of(2021, 6, 15), "Imprisonment"))
    assertThat(result.chargeRemand[2].chargeIds).isEqualTo(listOf(3L, 4L))

    assertThat(result.chargeRemand[3].from).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.chargeRemand[3].to).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.chargeRemand[3].fromEvent).isEqualTo(CourtAppearance(LocalDate.of(2019, 7, 6), "Commit/Transfer/Send to Crown Court for Trial in Custody"))
    assertThat(result.chargeRemand[3].toEvent).isEqualTo(CourtAppearance(LocalDate.of(2020, 9, 24), "Commit to Crown Court for Sentence Conditional Bail"))
    assertThat(result.chargeRemand[3].chargeIds).isEqualTo(listOf(2L))

    assertThat(result.adjustments).isNotEmpty
    assertThat(result.adjustments.size).isEqualTo(2)
    assertThat(result.adjustments[0].fromDate).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.adjustments[0].toDate).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.adjustments[1].fromDate).isEqualTo(LocalDate.of(2021, 6, 14))
    assertThat(result.adjustments[1].toDate).isEqualTo(LocalDate.of(2021, 6, 14))
  }

  @Test
  fun `Run calculation for an intersecting sentence`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.INTERSECTING_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.chargeRemand).isNotEmpty
    assertThat(result.chargeRemand.size).isEqualTo(2)
    assertThat(result.chargeRemand[0].from).isEqualTo(LocalDate.of(2020, 1, 1))
    assertThat(result.chargeRemand[0].to).isEqualTo(LocalDate.of(2021, 12, 31))
    assertThat(result.chargeRemand[0].chargeIds).isEqualTo(listOf(3933931L))
    assertThat(result.chargeRemand[1].from).isEqualTo(LocalDate.of(2021, 1, 1))
    assertThat(result.chargeRemand[1].to).isEqualTo(LocalDate.of(2021, 1, 31))
    assertThat(result.chargeRemand[1].chargeIds).isEqualTo(listOf(3933932L))
    assertThat(result.adjustments).isNotEmpty
    assertThat(result.adjustments.size).isEqualTo(3)
    assertThat(result.adjustments[0].fromDate).isEqualTo(LocalDate.of(2020, 1, 1))
    assertThat(result.adjustments[0].toDate).isEqualTo(LocalDate.of(2020, 12, 31))
    assertThat(result.adjustments[0].remand!!.chargeId).isEqualTo(listOf(3933931L))
    assertThat(result.adjustments[1].fromDate).isEqualTo(LocalDate.of(2021, 1, 1))
    assertThat(result.adjustments[1].toDate).isEqualTo(LocalDate.of(2021, 1, 31))
    assertThat(result.adjustments[1].remand!!.chargeId).isEqualTo(listOf(3933932L))
    assertThat(result.adjustments[2].fromDate).isEqualTo(LocalDate.of(2021, 4, 2))
    assertThat(result.adjustments[2].toDate).isEqualTo(LocalDate.of(2021, 12, 31))
    assertThat(result.adjustments[2].remand!!.chargeId).isEqualTo(listOf(3933931L))
  }

  @Test
  fun `Run calculation for an intersecting sentence where CRD returns validation messages`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.CRD_VALIDATION_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(result.userMessage).contains("Unable to calculation release dates on 2022-02-01")
  }

  @Test
  fun `Run calculation for a prisoner that has no offences dates`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.NO_OFFENCE_DATES}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(result.userMessage).contains("There are no offences with offence dates on the active booking.")
  }

  @Test
  fun `Save reject decision`() {
    webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.IMPRISONED_PRISONER}/decision")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        IdentifyRemandDecisionDto(
          accepted = false,
          rejectComment = "This is not correct",
        ),
      )
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isCreated

    val result = decisionRepository.findFirstByPersonOrderByDecisionAtDesc(PrisonApiExtension.IMPRISONED_PRISONER)

    assertThat(result!!.decisionByUsername).isEqualTo("test-client")
    assertThat(result.accepted).isFalse
    assertThat(result.rejectComment).isEqualTo("This is not correct")
    assertThat(result.days).isEqualTo(61)

    val getResult = webTestClient.get()
      .uri("/relevant-remand/${PrisonApiExtension.IMPRISONED_PRISONER}/decision")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isOk
      .returnResult(IdentifyRemandDecisionDto::class.java).responseBody.blockFirst()!!

    assertThat(getResult.decisionByPrisonDescription).isEqualTo("Birmingham Prison")
  }

  @Test
  fun `Save accept decision`() {
    webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.IMPRISONED_PRISONER}/decision")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        IdentifyRemandDecisionDto(
          accepted = true,
          rejectComment = null,
        ),
      )
      .headers(setAuthorisationRemandToolUser())
      .exchange()
      .expectStatus().isCreated

    val result = decisionRepository.findFirstByPersonOrderByDecisionAtDesc(PrisonApiExtension.IMPRISONED_PRISONER)

    assertThat(result!!.decisionByUsername).isEqualTo("test-client")
    assertThat(result.rejectComment).isEqualTo(null)
    assertThat(result.accepted).isTrue
    assertThat(result.days).isEqualTo(61)

    AdjustmentsApiExtension.adjustmentsApi.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/adjustments-api/adjustments")))
  }
}
