package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class Offence(
  val code: String,
  val statute: String,
  val description: String,
)
