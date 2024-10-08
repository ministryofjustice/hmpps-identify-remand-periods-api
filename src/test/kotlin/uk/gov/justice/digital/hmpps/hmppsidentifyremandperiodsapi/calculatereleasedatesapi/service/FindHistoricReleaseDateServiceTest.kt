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

    assertThat(exception.message).isEqualTo("The first calculation (2021-01-01T00:00) is over three weeks after sentence/recall calculation date date 2020-01-01.")
  }

  @Test
  fun `Preprod example where appeal made release before calculation date`() {
    val calculations = listOf(
      SentenceCalculationSummary(bookingId, 1, LocalDate.of(2021, 6, 3).atStartOfDay()),
      SentenceCalculationSummary(bookingId, 2, LocalDate.of(2021, 6, 14).atStartOfDay()),
    )
    val calculationOne = OffenderKeyDates(prisonerId, LocalDate.of(2021, 6, 3).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2021, 8, 31))
    val calculationTwo = OffenderKeyDates(prisonerId, LocalDate.of(2021, 6, 14).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2021, 6, 11))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(1)).thenReturn(calculationOne)
    whenever(apiClient.getNOMISOffenderKeyDates(2)).thenReturn(calculationTwo)

    val release = service.findReleaseDate(prisonerId, emptyList(), sentence, LocalDate.of(2021, 6, 2), emptyMap())

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2021, 6, 11))
    assertThat(release.calculationIds).isEqualTo(listOf(1L, 2L))
  }

  @Test
  fun `Release date is before calculate at`() {
    val calculateAt = sentenceDate
    val sentenceCalcId = 1L
    val actualCalculationTime = sentenceDate.atStartOfDay().plusDays(5)
    val calculations = listOf(SentenceCalculationSummary(bookingId, sentenceCalcId, actualCalculationTime))
    val calculation = OffenderKeyDates(prisonerId, actualCalculationTime, conditionalReleaseDate = calculateAt.minusDays(2))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(sentenceCalcId)).thenReturn(calculation)

    val exception = assertThrows<UnsupportedCalculationException> { service.findReleaseDate(prisonerId, emptyList(), sentence, calculateAt, emptyMap()) }

    assertThat(exception.message).isEqualTo("The release date is before the calculation date.")
  }

  @Test
  fun `Release is same day as calculation`() {
    val calculations = listOf(
      SentenceCalculationSummary(bookingId, 1, LocalDate.of(2019, 12, 4).atStartOfDay()),
      SentenceCalculationSummary(bookingId, 2, LocalDate.of(2020, 9, 23).atStartOfDay()),
    )
    val calculationOne = OffenderKeyDates(prisonerId, LocalDate.of(2019, 12, 4).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2020, 11, 21))
    val calculationTwo = OffenderKeyDates(prisonerId, LocalDate.of(2020, 9, 23).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2020, 9, 23))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(1)).thenReturn(calculationOne)
    whenever(apiClient.getNOMISOffenderKeyDates(2)).thenReturn(calculationTwo)

    val release = service.findReleaseDate(prisonerId, emptyList(), sentence, LocalDate.of(2019, 12, 1), emptyMap())

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2020, 9, 23))
    assertThat(release.calculationIds).isEqualTo(listOf(1L, 2L))
  }

  @Test
  fun `A new calculation on release date is not counted`() {
    val calculations = listOf(
      SentenceCalculationSummary(bookingId, 1, LocalDate.of(2023, 11, 8).atStartOfDay()),
      SentenceCalculationSummary(bookingId, 2, LocalDate.of(2024, 2, 7).atStartOfDay()),
    )
    val calculationOne = OffenderKeyDates(prisonerId, LocalDate.of(2023, 11, 8).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2024, 2, 7))
    val calculationTwo = OffenderKeyDates(prisonerId, LocalDate.of(2024, 2, 7).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2023, 12, 9))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(1)).thenReturn(calculationOne)
    whenever(apiClient.getNOMISOffenderKeyDates(2)).thenReturn(calculationTwo)

    val release = service.findReleaseDate(prisonerId, emptyList(), sentence, LocalDate.of(2023, 11, 6), emptyMap())

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2024, 2, 7))
    assertThat(release.calculationIds).isEqualTo(listOf(1L))
  }

  @Test
  fun `An immediate release`() {
    val calculations = listOf(
      SentenceCalculationSummary(bookingId, 1, LocalDate.of(2023, 7, 15).atStartOfDay()),
    )
    val calculationOne = OffenderKeyDates(prisonerId, LocalDate.of(2023, 7, 15).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2023, 7, 15))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(1)).thenReturn(calculationOne)

    val release = service.findReleaseDate(prisonerId, emptyList(), sentence, LocalDate.of(2023, 7, 15), emptyMap())

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2023, 7, 15))
    assertThat(release.calculationIds).isEqualTo(listOf(1L))
  }
}
