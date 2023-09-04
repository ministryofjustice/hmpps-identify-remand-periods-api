package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform.transform

data class LegacyDataProblem(
  val type: LegacyDataProblemType,
  val message: String,
  val offence: Offence,
  val bookingId: Long,
  val bookNumber: String,
  val courtCaseRef: String?,
) {
  constructor(type: LegacyDataProblemType, message: String, result: PrisonApiCourtDateResult) : this(type, message, transform(result.charge), result.bookingId, result.bookNumber, result.charge.courtCaseRef)
}
