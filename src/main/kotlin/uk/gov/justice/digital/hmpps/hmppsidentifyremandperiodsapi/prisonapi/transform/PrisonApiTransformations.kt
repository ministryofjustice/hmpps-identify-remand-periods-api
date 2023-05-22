package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Offence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import java.time.LocalDate

fun transform(results: List<PrisonApiCourtDateResult>, prisonerDetails: PrisonerDetails): RemandCalculation {
  val earliestActiveOffenceDate: LocalDate = findEarliestActiveOffenceDate(results, prisonerDetails)
  return RemandCalculation(
    prisonerDetails.offenderNo,
    results
      .filter { it.date.isAfter(earliestActiveOffenceDate) }
      .groupBy { it.charge.chargeId }
      .map {
        val charge = it.value.first().charge
        if (charge.offenceDate == null) {
          throw UnsupportedCalculationException("The charge ${charge.chargeId} has no offence date.")
        }
        ChargeAndEvents(
          Charge(
            it.key,
            transform(charge),
            charge.offenceDate,
            it.value.first().bookingId,
            charge.offenceEndDate,
            charge.sentenceSequence,
            charge.sentenceDate,
            charge.courtCaseRef,
            charge.courtLocation,
            charge.resultDescription,
          ),
          it.value.map { result -> transformToCourtDate(result) },
        )
      },
  )
}

private fun findEarliestActiveOffenceDate(results: List<PrisonApiCourtDateResult>, prisonerDetails: PrisonerDetails): LocalDate {
  return results
    .filter { it.bookingId == prisonerDetails.bookingId }
    .mapNotNull { it.charge.offenceDate }
    .min()
}

private fun transform(prisonApiCharge: PrisonApiCharge): Offence {
  return Offence(prisonApiCharge.offenceCode, prisonApiCharge.offenceStatue, prisonApiCharge.offenceDescription)
}

private fun transformToCourtDate(courtDateResult: PrisonApiCourtDateResult): CourtDate {
  return CourtDate(
    courtDateResult.date,
    transformToType(courtDateResult),
    courtDateResult.resultDispositionCode == "F",
  )
}

private fun transformToType(courtDateResult: PrisonApiCourtDateResult): CourtDateType {
  if (courtDateResult.resultCode == null) {
    throw UnsupportedCalculationException("The court event ${courtDateResult.id} has no outcome.")
  }
  return mapCourtDateResult(courtDateResult)
}
