package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.OffenderKeyDates
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.SentenceCalculationSummary
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service.FindHistoricReleaseDateService
import java.time.LocalDate

class FindHistoricReleaseDateServiceTest {

  private val apiClient = mock<PrisonApiClient>()

  private val service = FindHistoricReleaseDateService(apiClient)

  private val prisonerId = "ABC123"
  private val bookingId = 1L
  private val sentenceDate = LocalDate.of(2020, 1, 1)
  private val recallDate = LocalDate.of(2022, 1, 1)
  private val sentence = Sentence(1, sentenceDate, listOf(recallDate), bookingId)

  @Test
  fun `Successfully retrieve release date`() {
    val expectedReleaseDate = sentenceDate.plusYears(1)
    val calculateAt = sentenceDate
    val sentenceCalcId = 1L
    val actualCalculationTime = sentenceDate.atStartOfDay().plusDays(5)
    val calculations = listOf(SentenceCalculationSummary(bookingId, sentenceCalcId, actualCalculationTime))
    val calculation = OffenderKeyDates(prisonerId, actualCalculationTime, conditionalReleaseDate = expectedReleaseDate)
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(sentenceCalcId)).thenReturn(calculation)

    val release = service.findReleaseDate(prisonerId, emptyList(), sentence, calculateAt, emptyMap())

    assertThat(release.releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(release.calculationIds).isEqualTo(listOf(sentenceCalcId))
  }

  @Test
  fun `Release is before recall date`() {
    val expectedReleaseDate = sentenceDate.plusYears(10)
    val calculateAt = sentenceDate
    val sentenceCalcId = 1L
    val actualCalculationTime = sentenceDate.atStartOfDay().plusDays(5)
    val calculations = listOf(SentenceCalculationSummary(bookingId, sentenceCalcId, actualCalculationTime))
    val calculation = OffenderKeyDates(prisonerId, actualCalculationTime, conditionalReleaseDate = expectedReleaseDate)
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(sentenceCalcId)).thenReturn(calculation)

    val exception = assertThrows<UnsupportedCalculationException> { service.findReleaseDate(prisonerId, emptyList(), sentence, calculateAt, emptyMap()) }

    assertThat(exception.message).isEqualTo("Standard release date cannot be after recall date")
  }

  @Test
  fun `Unknown release date type fails`() {
    val expectedReleaseDate = sentenceDate.plusYears(1)
    val calculateAt = sentenceDate
    val sentenceCalcId = 1L
    val actualCalculationTime = sentenceDate.atStartOfDay().plusDays(5)
    val calculations = listOf(SentenceCalculationSummary(bookingId, sentenceCalcId, actualCalculationTime))
    val calculation = OffenderKeyDates(prisonerId, actualCalculationTime, paroleEligibilityDate = expectedReleaseDate)
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(sentenceCalcId)).thenReturn(calculation)

    val exception = assertThrows<UnsupportedCalculationException> { service.findReleaseDate(prisonerId, emptyList(), sentence, calculateAt, emptyMap()) }

    assertThat(exception.message).isEqualTo("Unable to find release date from calculations [1]")
  }

  @Test
  fun `Error if no release dates returned`() {
    val calculateAt = sentenceDate
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(emptyList())

    val exception = assertThrows<UnsupportedCalculationException> {
      service.findReleaseDate(
        prisonerId,
        emptyList(),
        sentence,
        calculateAt,
        emptyMap(),
      )
    }

    assertThat(exception.message).isEqualTo("No calculations found for ABC123 in booking 1")
  }

  @Test
  fun `Successfully retrieve release date when there are NOMIS blank release dates`() {
    val expectedReleaseDate = sentenceDate.plusYears(1)
    val calculateAt = sentenceDate
    val blankSentenceCalcId = 1L
    val expectedSentenceCalcId = 2L
    val blankCalculationTime = sentenceDate.atStartOfDay().plusHours(10).plusDays(5)
    val expectedCalculationTime = blankCalculationTime.minusMinutes(2)
    val calculations = listOf(SentenceCalculationSummary(bookingId, blankSentenceCalcId, blankCalculationTime), SentenceCalculationSummary(bookingId, expectedSentenceCalcId, expectedCalculationTime))
    val blankCalculation = OffenderKeyDates(prisonerId, blankCalculationTime)
    val expectedCalculation = OffenderKeyDates(prisonerId, expectedCalculationTime, conditionalReleaseDate = expectedReleaseDate)
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(blankSentenceCalcId)).thenReturn(blankCalculation)
    whenever(apiClient.getNOMISOffenderKeyDates(expectedSentenceCalcId)).thenReturn(expectedCalculation)

    val release = service.findReleaseDate(prisonerId, emptyList(), sentence, calculateAt, emptyMap())

    assertThat(release.releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(release.calculationIds).isEqualTo(listOf(1L, 2L))
  }

  @Test
  fun `First calculation date is much later than release date`() {
    val calculateAt = sentenceDate
    val sentenceCalcId = 1L
    val actualCalculationTime = sentenceDate.atStartOfDay().plusYears(1)
    val calculations = listOf(SentenceCalculationSummary(bookingId, sentenceCalcId, actualCalculationTime))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)

    val exception = assertThrows<UnsupportedCalculationException> { service.findReleaseDate(prisonerId, emptyList(), sentence, calculateAt, emptyMap()) }

    assertThat(exception.message).isEqualTo("The first calculation (2021-01-01T00:00) is over two weeks after sentence/recall calculation date date 2020-01-01.")
  }
}
