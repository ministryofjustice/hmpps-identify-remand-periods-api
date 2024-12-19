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
import java.time.LocalDateTime

@Service
class FindHistoricReleaseDateService(
  private val prisonApiClient: PrisonApiClient,
) : FindReleaseDateServiceProvider {

  override fun findReleaseDate(prisonerId: String, remand: List<Remand>, sentences: List<Sentence>, calculateAt: LocalDate, charges: Map<Long, Charge>): CalculationDetail {
    val allCalculations = prisonApiClient.getCalculationsForAPrisonerId(prisonerId).sortedBy { it.calculationDate }
    val historicReleaseDates = collapseByLastCalculationOfTheDay(allCalculations)
    if (historicReleaseDates.isEmpty()) {
      throw UnsupportedCalculationException("No calculations found for $prisonerId in bookings ${sentences.map { it.bookingId }}")
    }

    var calculation = historicReleaseDates.firstOrNull { it.calculationDate.toLocalDate().isAfterOrEqualTo(calculateAt) }
    if (calculation == null) {
      throw UnsupportedCalculationException("No calculations found for $prisonerId after sentence or recall date $calculateAt")
    }
    val calculationIds = mutableListOf<Long>()
    var releaseDate = getReleaseDateForCalcId(calculation.offenderSentCalculationId, calculation.calculationDate, allCalculations, calculationIds, calculateAt)
    if (releaseDate == calculation.calculationDate.toLocalDate()) {
      return CalculationDetail(releaseDate, calculationIds)
    }
    var lastCalculationBeforeRelease = historicReleaseDates.lastOrNull { it.calculationDate.isBefore(releaseDate.atStartOfDay()) }
    if (lastCalculationBeforeRelease == null) {
      // Immediate release
      return CalculationDetail(releaseDate, calculationIds)
    }
    while (lastCalculationBeforeRelease!!.offenderSentCalculationId != calculation!!.offenderSentCalculationId) {
      calculation = lastCalculationBeforeRelease
      releaseDate = getReleaseDateForCalcId(
        calculation.offenderSentCalculationId,
        calculation.calculationDate,
        allCalculations,
        calculationIds,
        calculateAt,
      )
      // On appeal the release is before the calculation date. (Okay as long as its after calculateAt)
      if (releaseDate.isBeforeOrEqualTo(calculation.calculationDate.toLocalDate())) {
        break
      }
      lastCalculationBeforeRelease = historicReleaseDates.last { it.calculationDate.toLocalDate().isBefore(releaseDate) }
    }
    return CalculationDetail(releaseDate, calculationIds.toList())
  }

  /*
    Flatten the list of release dates to only take the final calculation of the day.
   */
  private fun collapseByLastCalculationOfTheDay(
    historicReleaseDates: List<SentenceCalculationSummary>,
  ): List<SentenceCalculationSummary> {
    return historicReleaseDates
      .groupBy { it.calculationDate.toLocalDate() }.values.map { list -> list.maxBy { it.calculationDate } }
  }

  private fun getReleaseDateForCalcId(
    offenderSentCalcId: Long,
    calculationDate: LocalDateTime,
    allCalculations: List<SentenceCalculationSummary>,
    calculationIds: MutableList<Long>,
    calculateAt: LocalDate,
  ): LocalDate {
    val calcDates = prisonApiClient.getNOMISOffenderKeyDates(offenderSentCalcId)
    calculationIds.add(offenderSentCalcId)
    val releaseDates = listOfNotNull(
      calcDates.conditionalReleaseDate,
      calcDates.automaticReleaseDate,
      calcDates.postRecallReleaseDate,
      calcDates.midTermDate,
    )

    val latestRelease = releaseDates.maxOrNull()
    if (latestRelease != null) {
      if (latestRelease.isBefore(calculateAt)) {
        throw UnsupportedCalculationException("The release date $latestRelease, from calculations $calculationIds is before the calculation date $calculateAt.")
      }
      return latestRelease
    }

    // Release Date is blank. Seems to be a NOMIS bug, look at previous calculations for a non blank
    val latestPreviousCalculation = allCalculations.filter { it.calculationDate.isBefore(calculationDate) }
    if (latestPreviousCalculation.isNotEmpty()) {
      return getReleaseDateForCalcId(
        latestPreviousCalculation.last().offenderSentCalculationId,
        calculationDate,
        latestPreviousCalculation,
        calculationIds,
        calculateAt,
      )
    }
    throw UnsupportedCalculationException("Unable to find release date from calculations $calculationIds")
  }
}
