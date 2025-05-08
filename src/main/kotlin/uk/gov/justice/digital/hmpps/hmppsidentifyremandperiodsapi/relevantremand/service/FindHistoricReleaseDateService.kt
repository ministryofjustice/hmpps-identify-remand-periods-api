package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.SentenceCalculationSummary
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationDetail
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isAfterOrEqualTo
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isBeforeOrEqualTo
import java.time.LocalDate

@Service
class FindHistoricReleaseDateService(
  private val prisonApiClient: PrisonApiClient,
) : FindReleaseDateServiceProvider {

  override fun findReleaseDate(prisonerId: String, remand: List<Remand>, sentences: List<Sentence>, calculatedAt: LocalDate, charges: Map<Long, Charge>): CalculationDetail {
    val allCalculations = prisonApiClient.getCalculationsForAPrisonerId(prisonerId).sortedBy { it.calculationDate }
    val historicReleaseDates = collapseByLastCalculationOfTheDay(allCalculations)
    if (historicReleaseDates.isEmpty()) {
      throw UnsupportedCalculationException("No calculations found for $prisonerId in bookings ${sentences.map { it.bookingId }}")
    }

    var calculation = historicReleaseDates.firstOrNull { it.calculationDate.toLocalDate().isAfterOrEqualTo(calculatedAt) }
    if (calculation == null) {
      throw UnsupportedCalculationException("No calculations found for $prisonerId after sentence or recall date $calculatedAt")
    }
    val calculationIds = mutableListOf<Long>()
    var releaseDate = getReleaseDateForCalcId(calculation, allCalculations, calculationIds, calculatedAt)
    if (releaseDate == calculation.calculationDate.toLocalDate()) {
      return CalculationDetail(releaseDate, calculationIds)
    }
    var nextCalculation = historicReleaseDates.firstOrNull { it.calculationDate.isAfter(calculation!!.calculationDate) }
    while (nextCalculation != null && nextCalculation.calculationDate.toLocalDate().isBeforeOrEqualTo(releaseDate)) {
      calculation = nextCalculation
      releaseDate = getReleaseDateForCalcId(
        calculation,
        allCalculations,
        calculationIds,
        calculatedAt,
      )
      // On appeal the release is before the calculation date. (Okay as long as its after calculatedAt)
      if (releaseDate.isBeforeOrEqualTo(calculation.calculationDate.toLocalDate())) {
        break
      }
      nextCalculation = historicReleaseDates.firstOrNull { it.calculationDate.isAfter(calculation.calculationDate) }
    }
    return CalculationDetail(releaseDate, calculationIds.toList())
  }

  /*
    Flatten the list of release dates to only take the final calculation of the day.
   */
  private fun collapseByLastCalculationOfTheDay(
    historicReleaseDates: List<SentenceCalculationSummary>,
  ): List<SentenceCalculationSummary> = historicReleaseDates
    .filter { it.calculationReason != SentenceCalculationSummary.SENTENCE_DELETED_REASON }
    .groupBy { it.calculationDate.toLocalDate() }.values.map { list -> list.maxBy { it.calculationDate } }

  private fun getReleaseDateForCalcId(
    calculation: SentenceCalculationSummary,
    allCalculations: List<SentenceCalculationSummary>,
    calculationIds: MutableList<Long>,
    calculatedAt: LocalDate,
  ): LocalDate {
    val calcDates = prisonApiClient.getNOMISOffenderKeyDates(calculation.offenderSentCalculationId)
    calculationIds.add(calculation.offenderSentCalculationId)
    val calculatedReleaseDates = listOfNotNull(
      calcDates.conditionalReleaseDate,
      calcDates.automaticReleaseDate,
      calcDates.postRecallReleaseDate,
      calcDates.midTermDate,
    )
    val approvedParoleDate = calcDates.approvedParoleDate

    val latestRelease = approvedParoleDate ?: calculatedReleaseDates.maxOrNull()
    if (latestRelease != null) {
      if (latestRelease.isBefore(calculatedAt)) {
        throw UnsupportedCalculationException("The release date $latestRelease, from calculations $calculationIds is before the calculation date $calculatedAt.")
      }
      return latestRelease
    }

    // Release Date is blank. Seems to be a NOMIS bug, look at previous calculations for a non blank
    val latestPreviousCalculation = allCalculations.filter { it.calculationDate.isBefore(calculation.calculationDate) }
    if (latestPreviousCalculation.isNotEmpty()) {
      return getReleaseDateForCalcId(
        latestPreviousCalculation.last(),
        latestPreviousCalculation,
        calculationIds,
        calculatedAt,
      )
    }
    throw UnsupportedCalculationException("Unable to find release date from calculations $calculationIds")
  }
}
