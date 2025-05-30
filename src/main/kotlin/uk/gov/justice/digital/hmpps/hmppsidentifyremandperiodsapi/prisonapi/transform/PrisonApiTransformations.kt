package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform

import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateOutcome
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiExternalMovement
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiImprisonmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeLegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ExternalMovement
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Offence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isAfterOrEqualTo
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.mojDisplayFormat
import java.time.LocalDate

fun transform(results: List<PrisonApiCharge>, prisonerDetails: Prisoner, sentencesAndOffences: List<SentenceAndOffences>, imprisonmentStatus: List<PrisonApiImprisonmentStatus>, externalMovements: List<PrisonApiExternalMovement>): RemandCalculation {
  val earliestDateInActiveBooking: LocalDate = earliestDateInActiveBooking(results, prisonerDetails)
  val issuesWithLegacyData = mutableListOf<LegacyDataProblem>()
  val chargesFilteredByOffenceDate = filterEventsByOffenceDate(results, earliestDateInActiveBooking)

  return RemandCalculation(
    prisonerDetails.prisonerNumber,
    chargesFilteredByOffenceDate
      .map {
        ChargeAndEvents(
          Charge(
            it.chargeId,
            transform(it),
            it.bookingId,
            it.offenceDate,
            it.bookNumber,
            it.offenceEndDate,
            it.sentenceSequence,
            it.sentenceDate,
            it.courtCaseRef,
            it.courtLocation,
            it.resultDescription,
            it.resultDispositionCode == "F",
            it.bookingId == prisonerDetails.bookingId.toLong(),
            recallTypes.contains(it.sentenceType),
            termTypes.contains(it.sentenceType),
          ),
          it.outcomes.mapNotNull { result -> transformToCourtDate(result, it, issuesWithLegacyData) },
        )
      }
      .filter { it.dates.isNotEmpty() },
    imprisonmentStatus
      .filter { it.effectiveDate.isAfterOrEqualTo(earliestDateInActiveBooking) }
      .filter { it.toStatusType() != null }
      .map {
        ImprisonmentStatus(
          it.toStatusType()!!,
          it.effectiveDate,
        )
      }.sortedBy { it.date },
    sentencesAndOffences.flatMap { it.offences.map { offence -> offence.offenderChargeId } },
    issuesWithLegacyData,
    externalMovements.filter { it.movementDate.isAfter(earliestDateInActiveBooking) }.map { ExternalMovement(it.movementDate, it.movementType == "REL") }.sortedBy { it.date },
  )
}

fun filterEventsByOffenceDate(results: List<PrisonApiCharge>, earliestDateInActiveBooking: LocalDate): List<PrisonApiCharge> = results.map {
  it.copy(
    outcomes = it.outcomes.filter { result -> result.date.isAfterOrEqualTo(earliestDateInActiveBooking) },
  )
}.filter { it.outcomes.isNotEmpty() }

private fun earliestDateInActiveBooking(results: List<PrisonApiCharge>, prisonerDetails: Prisoner): LocalDate = results
  .filter { it.bookingId == prisonerDetails.bookingId.toLong() }
  .flatMap { it.outcomes.filter { outcome -> outcome.resultCode != null }.map { outcome -> outcome.date } + it.offenceDate }
  .filterNotNull()
  .ifEmpty {
    throw UnsupportedCalculationException("There are no offences with offence dates on the active booking.")
  }
  .min()

fun transform(prisonApiCharge: PrisonApiCharge): Offence = Offence(prisonApiCharge.offenceCode, prisonApiCharge.offenceStatue, prisonApiCharge.offenceDescription)

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
    issuesWithLegacyData.add(ChargeLegacyDataProblem(LegacyDataProblemType.MISSING_COURT_OUTCOME, "The court hearing on ${courtDateResult.date.mojDisplayFormat()} for '${charge.offenceDescription}' has a missing hearing outcome within booking ${charge.bookNumber}.", charge))
    return null
  }
  return mapCourtDateResult(courtDateResult, charge, issuesWithLegacyData)
}

// TODO ADJST-1178 Use remand and sentencing api to get recall and term types.
private val recallTypes = listOf(
  "LR",
  "LR_ORA",
  "LR_YOI_ORA",
  "LR_SEC91_ORA",
  "LRSEC250_ORA",
  "LR_EDS18",
  "LR_EDS21",
  "LR_EDSU18",
  "LR_LASPO_AR",
  "LR_LASPO_DR",
  "LR_SEC236A",
  "LR_SOPC18",
  "LR_SOPC21",
  "14FTR_ORA",
  "FTR",
  "FTR_ORA",
  "FTR_SCH15",
  "FTRSCH15_ORA",
  "FTRSCH18",
  "FTRSCH18_ORA",
  "14FTRHDC_ORA",
  "FTR_HDC_ORA",
)

private val termTypes = listOf(
  "A/FINE",
  "DTO",
  "DTO_ORA",
  "BOTUS",
)
