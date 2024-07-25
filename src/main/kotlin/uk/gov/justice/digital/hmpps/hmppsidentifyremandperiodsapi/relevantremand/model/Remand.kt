package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class Remand(
  override val from: LocalDate,
  override val to: LocalDate,
  val chargeId: Long,
) : Period
