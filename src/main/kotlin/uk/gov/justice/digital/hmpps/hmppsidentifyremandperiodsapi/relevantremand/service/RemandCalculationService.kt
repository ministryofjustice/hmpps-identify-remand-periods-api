package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.CONTINUE
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.START
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.STOP
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RelatedCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.time.LocalDate

@Service
class RemandCalculationService(
  private val sentenceRemandService: SentenceRemandService,
) {

  fun calculate(remandCalculation: RemandCalculation): RemandResult {
    if (remandCalculation.charges.isEmpty()) {
      throw UnsupportedCalculationException("There are no charges to calculate")
    }
    val charges = combineRelatedCharges(remandCalculation)
    val chargeRemand = remandClock(charges)
    val sentenceDates = remandCalculation.charges
      .filter { it.charge.sentenceDate != null && it.charge.sentenceSequence != null }
      .map { Sentence(it.charge.sentenceSequence!!, it.charge.sentenceDate!!, it.charge.bookingId) }
      .distinctBy { it.sentenceDate }
    val sentenceRemand = sentenceRemandService.extractSentenceRemand(remandCalculation.prisonerId, chargeRemand, sentenceDates)
    return RemandResult(chargeRemand, sentenceRemand)
  }

  private fun remandClock(remandCalculation: RemandCalculation): List<Remand> {
    val remand = mutableListOf<Remand>()
    remandCalculation.charges.forEach { chargeAndEvent ->
      if (hasAnyRemandEvent(chargeAndEvent.dates)) {
        var from: LocalDate? = null
        chargeAndEvent.dates.forEach {
          if (listOf(START, CONTINUE).contains(it.type) && from == null) {
            from = it.date
          }
          if (it.type == STOP && from != null) {
            remand.add(Remand(from!!, getToDate(it), chargeAndEvent.charge))
            from = null
          }
        }
      }
    }
    return remand
  }

  private fun combineRelatedCharges(remandCalculation: RemandCalculation): RemandCalculation {
    val mapOfRelatedCharges = remandCalculation.charges.groupBy {
      RelatedCharge(
        it.charge.offenceDate,
        it.charge.offenceEndDate,
        it.charge.offence.code,
      )
    }
    return remandCalculation.copy(
      charges = mapOfRelatedCharges.map {
        ChargeAndEvents(pickMostAppropriateCharge(it.value), flattenCourtDates(it.value))
      },
    )
  }

  private fun pickMostAppropriateCharge(relatedCharges: List<ChargeAndEvents>): Charge {
    // Pick the charge with a sentence attached, otherwise just the first charge. This logic may change.
    return relatedCharges.find { it.charge.sentenceSequence != null }?.charge ?: relatedCharges.first().charge
  }

  private fun flattenCourtDates(relatedCharges: List<ChargeAndEvents>) =
    relatedCharges.flatMap { it.dates }.sortedBy { it.date }

  private fun hasAnyRemandEvent(courtDates: List<CourtDate>) = courtDates.any { listOf(START, CONTINUE).contains(it.type) }

  private fun getToDate(courtDate: CourtDate) = if (courtDate.final) courtDate.date.minusDays(1) else courtDate.date
}
