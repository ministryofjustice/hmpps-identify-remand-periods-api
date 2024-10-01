package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class RemandApplicableUserSelection(
  val chargeIdsToMakeApplicable: List<Long>,
  val targetChargeId: Long,
)
