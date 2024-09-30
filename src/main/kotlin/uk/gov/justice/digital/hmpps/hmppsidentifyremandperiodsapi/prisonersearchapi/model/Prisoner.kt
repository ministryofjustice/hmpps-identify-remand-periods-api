package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonersearchapi.model

data class Prisoner(
  val prisonerNumber: String,
  val bookingId: String,
  val prisonId: String,
)
