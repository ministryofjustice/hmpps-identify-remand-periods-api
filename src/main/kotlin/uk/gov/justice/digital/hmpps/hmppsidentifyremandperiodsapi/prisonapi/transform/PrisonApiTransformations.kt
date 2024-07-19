package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform

import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateOutcome
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

fun transform(results: List<PrisonApiCharge>, prisonerDetails: PrisonerDetails, sentencesAndOffences: List<SentenceAndOffences>): RemandCalculation {
  val earliestActiveOffenceDate: LocalDate = findEarliestActiveOffenceDate(results, prisonerDetails)
  val issuesWithLegacyData = mutableListOf<LegacyDataProblem>()
  return RemandCalculation(
    prisonerDetails.offenderNo,
    results
      .filter {
        if (it.offenceDate == null) {
          issuesWithLegacyData.add(LegacyDataProblem(LegacyDataProblemType.MISSING_OFFENCE_DATE, "There is another offence of '${it.offenceDescription}' within booking ${it.bookNumber} that has a missing offence date.", it))
          false
        } else {
          true
        }
      }
      .map {
        ChargeAndEvents(
          Charge(
            it.chargeId,
            transform(it),
            it.offenceDate!!,
            it.bookingId,
            it.bookNumber,
            it.offenceEndDate,
            it.sentenceSequence,
            it.sentenceDate,
            it.courtCaseRef,
            it.courtLocation,
            it.resultDescription,
            it.bookingId == prisonerDetails.bookingId,
          ),
          it.outcomes
            .filter { result -> result.date.isAfter(earliestActiveOffenceDate) }
            .mapNotNull { result -> transformToCourtDate(result, it, issuesWithLegacyData) },
        )
      }
      .filter { it.dates.isNotEmpty() },
    sentencesAndOffences.flatMap { it.offences.map { offence -> offence.offenderChargeId } },
    issuesWithLegacyData,
  )
}

private fun findEarliestActiveOffenceDate(results: List<PrisonApiCharge>, prisonerDetails: PrisonerDetails): LocalDate {
  return results
    .filter { it.bookingId == prisonerDetails.bookingId }
    .mapNotNull { it.offenceDate }
    .ifEmpty {
      throw UnsupportedCalculationException("There are no offences with offence dates on the active booking.")
    }
    .min()
}

fun transform(prisonApiCharge: PrisonApiCharge): Offence {
  return Offence(prisonApiCharge.offenceCode, prisonApiCharge.offenceStatue, prisonApiCharge.offenceDescription)
}

private fun transformToCourtDate(courtDateResult: PrisonApiCourtDateOutcome, charge: PrisonApiCharge, issuesWithLegacyData: MutableList<LegacyDataProblem>): CourtDate? {
  val type = transformToType(courtDateResult, charge, issuesWithLegacyData)
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

private fun transformToType(courtDateResult: PrisonApiCourtDateOutcome, charge: PrisonApiCharge, issuesWithLegacyData: MutableList<LegacyDataProblem>): CourtDateType? {
  if (courtDateResult.resultCode == null) {
    issuesWithLegacyData.add(LegacyDataProblem(LegacyDataProblemType.MISSING_COURT_OUTCOME, "The court hearing on ${courtDateResult.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} for '${charge.offenceDescription}' has a missing hearing outcome within booking ${charge.bookNumber}.", charge))
    return null
  }
  return mapCourtDateResult(courtDateResult, charge, issuesWithLegacyData)
}
