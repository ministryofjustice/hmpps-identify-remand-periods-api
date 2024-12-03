package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform.transform

data class ChargeLegacyDataProblem(
  override val type: LegacyDataProblemType,
  override val message: String,
  val offence: Offence,
  val bookingId: Long,
  val bookNumber: String,
  val courtCaseRef: String?,
  override val developerMessage: String? = null,
) : LegacyDataProblem {
  constructor(type: LegacyDataProblemType, message: String, charge: PrisonApiCharge) : this(type, message, transform(charge), charge.bookingId, charge.bookNumber, charge.courtCaseRef)
  constructor(type: LegacyDataProblemType, message: String, charge: Charge, developerMessage: String?) : this(type, message, charge.offence, charge.bookingId, charge.bookNumber, charge.courtCaseRef, developerMessage)
  constructor(type: LegacyDataProblemType, message: String, charge: Charge) : this(type, message, charge, null)
}
