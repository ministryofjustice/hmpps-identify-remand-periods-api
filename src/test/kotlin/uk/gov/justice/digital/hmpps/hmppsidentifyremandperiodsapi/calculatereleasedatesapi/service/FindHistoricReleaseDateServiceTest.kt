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
import java.time.LocalDate

class FindHistoricReleaseDateServiceTest {

  private val apiClient = mock<PrisonApiClient>()

  private val service = FindHistoricReleaseDateService(apiClient)

  private val prisonerId = "ABC123"
  private val bookingId = 1L
  private val sentenceDate = LocalDate.of(2022, 1, 1)
  private val recallDate = LocalDate.of(2022, 1, 1)
  private val sentence = Sentence(1, sentenceDate, recallDate, bookingId)

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

    val release = service.calculateReleaseDate(prisonerId, emptyList(), sentence, calculateAt)

    assertThat(release).isEqualTo(expectedReleaseDate)
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

    val exception = assertThrows<UnsupportedCalculationException> { service.calculateReleaseDate(prisonerId, emptyList(), sentence, calculateAt) }

    assertThat(exception.message).isEqualTo("Unable to find release date from calculation 1")
  }
}
