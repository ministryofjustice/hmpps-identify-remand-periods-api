package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.SentenceCalculationSummary
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.time.LocalDate

@Service
class FindHistoricReleaseDateService(
  private val prisonApiClient: PrisonApiClient,
) {

  fun calculateReleaseDate(prisonerId: String, remand: List<Remand>, sentence: Sentence, calculateAt: LocalDate): LocalDate {
    val historicReleaseDates = collapseByLastCalculationOfTheDay(prisonApiClient.getCalculationsForAPrisonerId(prisonerId).sortedBy { it.calculationDate }, sentence)
    if (historicReleaseDates.isEmpty()) {
      return calculateAt
    }

    var calculation = historicReleaseDates.first { it.calculationDate.isAfter(calculateAt.atStartOfDay()) }
    var releaseDate = getReleaseDateForCalcId(calculation.offenderSentCalculationId)
    var lastCalculationBeforeRelease = historicReleaseDates.last { it.calculationDate.isBefore(releaseDate.atStartOfDay()) }
    while (lastCalculationBeforeRelease.offenderSentCalculationId != calculation.offenderSentCalculationId) {
      calculation = lastCalculationBeforeRelease
      releaseDate = getReleaseDateForCalcId(calculation.offenderSentCalculationId)
      lastCalculationBeforeRelease = historicReleaseDates.last { it.calculationDate.isBefore(releaseDate.atStartOfDay()) }
    }
    return releaseDate
  }

  /*
    Flatten the list of release dates to only take the final calculation of the day.
   */
  private fun collapseByLastCalculationOfTheDay(
    historicReleaseDates: List<SentenceCalculationSummary>,
    sentence: Sentence,
  ): List<SentenceCalculationSummary> {
    return historicReleaseDates
      .filter { it.bookingId == sentence.bookingId }
      .groupBy { it.calculationDate.toLocalDate() }.values.map { list -> list.maxBy { it.calculationDate } }
  }

  private fun getReleaseDateForCalcId(offenderSentCalcId: Long): LocalDate {
    val calcDates = prisonApiClient.getNOMISOffenderKeyDates(offenderSentCalcId)
    val releaseDates = listOfNotNull(calcDates.conditionalReleaseDate, calcDates.automaticReleaseDate, calcDates.postRecallReleaseDate, calcDates.midTermDate)

    return releaseDates.max()
  }
}
