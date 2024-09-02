package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.SentenceCalculationSummary
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.time.LocalDate

@Service
class FindHistoricReleaseDateService(
  private val prisonApiClient: PrisonApiClient,
) : FindReleaseDateServiceProvider {

  override fun findReleaseDate(prisonerId: String, remand: List<Remand>, sentence: Sentence, calculateAt: LocalDate, charges: Map<Long, Charge>): LocalDate {
    val historicReleaseDates = collapseByLastCalculationOfTheDay(prisonApiClient.getCalculationsForAPrisonerId(prisonerId).sortedBy { it.calculationDate }, sentence)
    if (historicReleaseDates.isEmpty()) {
      throw UnsupportedCalculationException("No calculations found for $prisonerId in booking ${sentence.bookingId}")
    }

    var calculation = historicReleaseDates.firstOrNull { it.calculationDate.isAfter(calculateAt.atStartOfDay()) }
    if (calculation == null) {
      throw UnsupportedCalculationException("No calculations found for $prisonerId after sentence or recall date $calculateAt")
    }
    var releaseDate = getReleaseDateForCalcId(calculation.offenderSentCalculationId)
    var lastCalculationBeforeRelease = historicReleaseDates.last { it.calculationDate.isBefore(releaseDate.atStartOfDay()) }
    while (lastCalculationBeforeRelease.offenderSentCalculationId != calculation!!.offenderSentCalculationId) {
      calculation = lastCalculationBeforeRelease
      releaseDate = getReleaseDateForCalcId(calculation.offenderSentCalculationId)
      lastCalculationBeforeRelease = historicReleaseDates.last { it.calculationDate.isBefore(releaseDate.atStartOfDay()) }
    }
    if (releaseDate.atStartOfDay().isBefore(lastCalculationBeforeRelease.calculationDate)) {
      throw UnsupportedCalculationException("Release date cannot be before calculation date")
    }
    if (calculateAt == sentence.sentenceDate && sentence.recallDate != null && releaseDate.isAfter(sentence.recallDate)) {
      throw UnsupportedCalculationException("Standard release date cannot be after recall date")
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

    val latestRelease = releaseDates.maxOrNull()

    if (latestRelease == null) {
      throw UnsupportedCalculationException("Unable to find release date from calculation $offenderSentCalcId")
    } else {
      return latestRelease
    }
  }
}
