package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Offence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun transform(results: List<PrisonApiCourtDateResult>, prisonerDetails: PrisonerDetails): RemandCalculation {
  val earliestActiveOffenceDate: LocalDate = findEarliestActiveOffenceDate(results, prisonerDetails)
  val issuesWithLegacyData = mutableListOf<LegacyDataProblem>()
  return RemandCalculation(
    prisonerDetails.offenderNo,
    results
      .filter { it.date.isAfter(earliestActiveOffenceDate) }
      .groupBy { it.charge.chargeId }
      .filter {
        if (it.value.first().charge.offenceDate == null) {
          issuesWithLegacyData.add(LegacyDataProblem(LegacyDataProblemType.MISSING_OFFENCE_DATE, "There is another offence of '${it.value.first().charge.offenceDescription}' within booking ${it.value.first().bookNumber} that has a missing offence date.", it.value.first()))
          false
        } else {
          true
        }
      }
      .map {
        val charge = it.value.first().charge
        ChargeAndEvents(
          Charge(
            it.key,
            transform(charge),
            charge.offenceDate!!,
            it.value.first().bookingId,
            it.value.first().bookNumber,
            charge.offenceEndDate,
            charge.sentenceSequence,
            charge.sentenceDate,
            charge.courtCaseRef,
            charge.courtLocation,
            charge.resultDescription,
          ),
          it.value.mapNotNull { result -> transformToCourtDate(result, issuesWithLegacyData) },
        )
      },
    issuesWithLegacyData,
  )
}

private fun findEarliestActiveOffenceDate(results: List<PrisonApiCourtDateResult>, prisonerDetails: PrisonerDetails): LocalDate {
  return results
    .filter { it.bookingId == prisonerDetails.bookingId }
    .mapNotNull { it.charge.offenceDate }
    .ifEmpty {
      throw UnsupportedCalculationException("There are no offences with offence dates on the active booking.")
    }
    .min()
}

public fun transform(prisonApiCharge: PrisonApiCharge): Offence {
  return Offence(prisonApiCharge.offenceCode, prisonApiCharge.offenceStatue, prisonApiCharge.offenceDescription)
}

private fun transformToCourtDate(courtDateResult: PrisonApiCourtDateResult, issuesWithLegacyData: MutableList<LegacyDataProblem>): CourtDate? {
  val type = transformToType(courtDateResult, issuesWithLegacyData)
  if (type != null) {
    return CourtDate(
      courtDateResult.date,
      type,
      courtDateResult.resultDescription!!,
      courtDateResult.resultDispositionCode == "F",
      courtDateResult.resultCode == RECALL_COURT_EVENT,
      isEventCustodial(courtDateResult),
    )
  }
  return null
}

private fun transformToType(courtDateResult: PrisonApiCourtDateResult, issuesWithLegacyData: MutableList<LegacyDataProblem>): CourtDateType? {
  if (courtDateResult.resultCode == null) {
    issuesWithLegacyData.add(LegacyDataProblem(LegacyDataProblemType.MISSING_COURT_OUTCOME, "The court hearing on ${courtDateResult.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} for '${courtDateResult.charge.offenceDescription}' has a missing hearing outcome within booking ${courtDateResult.bookNumber}.", courtDateResult))
    return null
  }
  return mapCourtDateResult(courtDateResult, issuesWithLegacyData)
}
