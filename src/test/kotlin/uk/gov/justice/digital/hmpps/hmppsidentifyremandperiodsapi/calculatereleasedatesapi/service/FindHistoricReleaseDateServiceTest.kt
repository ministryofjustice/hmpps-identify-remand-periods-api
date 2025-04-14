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
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Offence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service.FindHistoricReleaseDateService
import java.time.LocalDate

class FindHistoricReleaseDateServiceTest {

  private val apiClient = mock<PrisonApiClient>()

  private val service = FindHistoricReleaseDateService(apiClient)

  private val prisonerId = "ABC123"
  private val bookingId = 1L
  private val bookNumber = "QRS321"
  private val sentenceDate = LocalDate.of(2020, 1, 1)
  private val recallDate = LocalDate.of(2022, 1, 1)
  private val sentence = Sentence(1, sentenceDate, listOf(recallDate), bookingId)
  private val charges = mapOf(1L to Charge(1L, Offence("ABC", "123", "An offence"), bookingId, bookNumber = bookNumber))

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

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), calculateAt, charges)

    assertThat(release.releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(release.calculationIds).isEqualTo(listOf(sentenceCalcId))
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

    val exception = assertThrows<UnsupportedCalculationException> { service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), calculateAt, charges) }

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
        listOf(sentence),
        calculateAt,
        charges,
      )
    }

    assertThat(exception.message).isEqualTo("No calculations found for ABC123 in bookings [1]")
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

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), calculateAt, charges)

    assertThat(release.releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(release.calculationIds).isEqualTo(listOf(1L, 2L))
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

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), LocalDate.of(2021, 6, 2), charges)

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

    val exception = assertThrows<UnsupportedCalculationException> { service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), calculateAt, charges) }

    assertThat(exception.message).isEqualTo("The release date 2019-12-30, from calculations [1] is before the calculation date 2020-01-01.")
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

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), LocalDate.of(2019, 12, 1), charges)

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2020, 9, 23))
    assertThat(release.calculationIds).isEqualTo(listOf(1L, 2L))
  }

  @Test
  fun `An immediate release`() {
    val calculations = listOf(
      SentenceCalculationSummary(bookingId, 1, LocalDate.of(2023, 7, 15).atStartOfDay()),
    )
    val calculationOne = OffenderKeyDates(prisonerId, LocalDate.of(2023, 7, 15).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2023, 7, 15))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(1)).thenReturn(calculationOne)

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), LocalDate.of(2023, 7, 15), charges)

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2023, 7, 15))
    assertThat(release.calculationIds).isEqualTo(listOf(1L))
  }

  @Test
  fun `Find historic release date from merged booking`() {
    val oldBookingId = 99L
    val oldCharge = charges[1]!!.copy(chargeId = 99L, bookingId = oldBookingId, bookNumber = bookNumber) // Old booking has same bookNumber, so is a merge
    val bothCharges = charges.toMutableMap()
    bothCharges[99L] = oldCharge
    val calculations = listOf(
      SentenceCalculationSummary(oldBookingId, 1, LocalDate.of(2020, 1, 1).atStartOfDay()),
      SentenceCalculationSummary(oldBookingId, 2, LocalDate.of(2020, 6, 1).atStartOfDay()),
      SentenceCalculationSummary(bookingId, 3, LocalDate.of(2024, 10, 1).atStartOfDay()),
    )

    val calculationOne = OffenderKeyDates(prisonerId, LocalDate.of(2020, 1, 1).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2021, 1, 1))
    val calculationTwo = OffenderKeyDates(prisonerId, LocalDate.of(2020, 6, 1).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2021, 1, 1))
    val calculationThree = OffenderKeyDates(prisonerId, LocalDate.of(2024, 10, 1).atStartOfDay(), conditionalReleaseDate = LocalDate.of(2023, 7, 15))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(1)).thenReturn(calculationOne)
    whenever(apiClient.getNOMISOffenderKeyDates(2)).thenReturn(calculationTwo)
    whenever(apiClient.getNOMISOffenderKeyDates(3)).thenReturn(calculationThree)

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), LocalDate.of(202, 1, 2), bothCharges)

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2021, 1, 1))
    assertThat(release.calculationIds).isEqualTo(listOf(1L, 2L))
  }

  @Test
  fun `First calculation is on same day as release`() {
    val calculations = listOf(
      SentenceCalculationSummary(bookingId, 1, LocalDate.of(2024, 10, 29).atStartOfDay()),
    )
    val calculationOne = OffenderKeyDates(prisonerId, LocalDate.of(2024, 10, 29).atStartOfDay(), postRecallReleaseDate = LocalDate.of(2024, 10, 29))
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(1)).thenReturn(calculationOne)

    val aSentence = Sentence(1, LocalDate.of(2024, 5, 13), listOf(LocalDate.of(2024, 10, 28)), bookingId)

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(aSentence), LocalDate.of(2024, 10, 28), charges)

    assertThat(release.releaseDate).isEqualTo(LocalDate.of(2024, 10, 29))
    assertThat(release.calculationIds).isEqualTo(listOf(1L))
  }

  @Test
  fun `Ignore sentence deleted calculations`() {
    val expectedReleaseDate = sentenceDate.plusYears(1)
    val calculateAt = sentenceDate
    val sentenceCalcId = 1L
    val actualCalculationTime = sentenceDate.atStartOfDay().plusDays(5)
    val actualCalculation = SentenceCalculationSummary(bookingId, sentenceCalcId, actualCalculationTime)
    val sentenceDeletedCalculation = SentenceCalculationSummary(bookingId, 2L, sentenceDate.atStartOfDay().plusDays(4), calculationReason = SentenceCalculationSummary.SENTENCE_DELETED_REASON)
    val calculations = listOf(sentenceDeletedCalculation, actualCalculation)
    val calculation = OffenderKeyDates(prisonerId, actualCalculationTime, conditionalReleaseDate = expectedReleaseDate)
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(sentenceCalcId)).thenReturn(calculation)

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), calculateAt, charges)

    assertThat(release.releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(release.calculationIds).isEqualTo(listOf(sentenceCalcId))
  }

  @Test
  fun `APD date is before PRRD`() {
    val postRecallReleaseDate = sentenceDate.plusYears(2)
    val expectedReleaseDate = sentenceDate.plusYears(1)
    val calculateAt = sentenceDate
    val sentenceCalcId = 1L
    val actualCalculationTime = sentenceDate.atStartOfDay().plusDays(5)
    val calculations = listOf(SentenceCalculationSummary(bookingId, sentenceCalcId, actualCalculationTime))
    val calculation = OffenderKeyDates(prisonerId, actualCalculationTime, postRecallReleaseDate = postRecallReleaseDate, approvedParoleDate = expectedReleaseDate)
    whenever(apiClient.getCalculationsForAPrisonerId(prisonerId)).thenReturn(calculations)
    whenever(apiClient.getNOMISOffenderKeyDates(sentenceCalcId)).thenReturn(calculation)

    val release = service.findReleaseDate(prisonerId, emptyList(), listOf(sentence), calculateAt, charges)

    assertThat(release.releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(release.calculationIds).isEqualTo(listOf(sentenceCalcId))
  }
}
