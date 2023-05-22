package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class RelatedCharge(
  val offenceDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val offenceCode: String,
)
