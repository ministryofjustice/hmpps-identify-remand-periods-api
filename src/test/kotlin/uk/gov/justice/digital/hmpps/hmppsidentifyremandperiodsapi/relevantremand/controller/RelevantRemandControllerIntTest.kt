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
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.IdentifyRemandDecisionDto
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
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.sentenceRemand).isNotEmpty
    assertThat(result.sentenceRemand[0].from).isEqualTo(LocalDate.of(2022, 10, 13))
    assertThat(result.sentenceRemand[0].to).isEqualTo(LocalDate.of(2022, 12, 12))
    assertThat(result.sentenceRemand[0].days).isEqualTo(61)
    assertThat(result.issuesWithLegacyData).isEqualTo(listOf("The court event on 13 Dec 2022 for offence An offence committed at 5 May 2021 has a missing outcome within booking ABC123"))
  }

  @Test
  fun `Run calculation for a bail prisoner`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.BAIL_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.sentenceRemand).isNotEmpty
    assertThat(result.sentenceRemand[0].from).isEqualTo(LocalDate.of(2015, 3, 25))
    assertThat(result.sentenceRemand[0].to).isEqualTo(LocalDate.of(2015, 4, 8))
    assertThat(result.sentenceRemand[0].days).isEqualTo(15)
  }

  @Test
  fun `Run calculation for a prisoner with related offences`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.RELATED_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.sentenceRemand).isNotEmpty
    assertThat(result.sentenceRemand[0].from).isEqualTo(LocalDate.of(2015, 3, 20))
    assertThat(result.sentenceRemand[0].to).isEqualTo(LocalDate.of(2015, 4, 10))
    assertThat(result.sentenceRemand[0].days).isEqualTo(22)
  }

  @Test
  fun `Run calculation for a prisoner with multiple offences`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.MULTIPLE_OFFENCES_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.chargeRemand).isNotEmpty
    assertThat(result.chargeRemand.size).isEqualTo(5)
    assertThat(result.chargeRemand[0].from).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.chargeRemand[0].to).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.chargeRemand[1].from).isEqualTo(LocalDate.of(2021, 6, 14))
    assertThat(result.chargeRemand[1].to).isEqualTo(LocalDate.of(2021, 6, 14))
    assertThat(result.chargeRemand[2].from).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.chargeRemand[2].to).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.chargeRemand[3].from).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.chargeRemand[3].to).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.chargeRemand[4].from).isEqualTo(LocalDate.of(2021, 6, 14))
    assertThat(result.chargeRemand[4].to).isEqualTo(LocalDate.of(2021, 6, 14))

    assertThat(result.sentenceRemand).isNotEmpty
    assertThat(result.sentenceRemand.size).isEqualTo(2)
    assertThat(result.sentenceRemand[0].from).isEqualTo(LocalDate.of(2019, 7, 6))
    assertThat(result.sentenceRemand[0].to).isEqualTo(LocalDate.of(2020, 9, 24))
    assertThat(result.sentenceRemand[1].from).isEqualTo(LocalDate.of(2021, 6, 14))
    assertThat(result.sentenceRemand[1].to).isEqualTo(LocalDate.of(2021, 6, 14))
  }

  @Test
  fun `Run calculation for an intersecting sentence`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.INTERSECTING_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RemandResult::class.java)
      .returnResult().responseBody!!

    assertThat(result.chargeRemand).isNotEmpty
    assertThat(result.chargeRemand.size).isEqualTo(2)
    assertThat(result.chargeRemand[0].from).isEqualTo(LocalDate.of(2020, 1, 1))
    assertThat(result.chargeRemand[0].to).isEqualTo(LocalDate.of(2021, 12, 31))
    assertThat(result.chargeRemand[0].charge.sentenceSequence).isEqualTo(3)
    assertThat(result.chargeRemand[1].from).isEqualTo(LocalDate.of(2021, 1, 1))
    assertThat(result.chargeRemand[1].to).isEqualTo(LocalDate.of(2021, 1, 31))
    assertThat(result.chargeRemand[1].charge.sentenceSequence).isEqualTo(4)
    assertThat(result.sentenceRemand).isNotEmpty
    assertThat(result.sentenceRemand.size).isEqualTo(3)
    assertThat(result.sentenceRemand[0].from).isEqualTo(LocalDate.of(2021, 1, 1))
    assertThat(result.sentenceRemand[0].to).isEqualTo(LocalDate.of(2021, 1, 31))
    assertThat(result.sentenceRemand[0].charge.sentenceSequence).isEqualTo(4)
    assertThat(result.sentenceRemand[1].from).isEqualTo(LocalDate.of(2020, 1, 1))
    assertThat(result.sentenceRemand[1].to).isEqualTo(LocalDate.of(2020, 12, 31))
    assertThat(result.sentenceRemand[1].charge.sentenceSequence).isEqualTo(3)
    assertThat(result.sentenceRemand[2].from).isEqualTo(LocalDate.of(2021, 4, 2))
    assertThat(result.sentenceRemand[2].to).isEqualTo(LocalDate.of(2021, 12, 31))
    assertThat(result.sentenceRemand[2].charge.sentenceSequence).isEqualTo(3)
  }

  @Test
  fun `Run calculation for an intersecting sentence where CRD returns validation messages`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.CRD_VALIDATION_PRISONER}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(result.userMessage).contains("Unsupported sentence type 2020 Uknown")
  }

  @Test
  fun `Run calculation for a prisoner that has no offences dates`() {
    val result = webTestClient.post()
      .uri("/relevant-remand/${PrisonApiExtension.NO_OFFENCE_DATES}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
      .exchange()
      .expectStatus().isCreated

    val result = decisionRepository.findFirstByPersonOrderByDecisionAtDesc(PrisonApiExtension.IMPRISONED_PRISONER)

    assertThat(result!!.decisionByUsername).isEqualTo("test-client")
    assertThat(result.accepted).isFalse
    assertThat(result.rejectComment).isEqualTo("This is not correct")
    assertThat(result.days).isEqualTo(61)
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
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_DIGITAL_WARRANT")))
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
