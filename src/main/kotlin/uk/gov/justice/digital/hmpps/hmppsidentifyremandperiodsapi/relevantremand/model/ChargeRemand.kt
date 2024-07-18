package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class ChargeRemand(
  override val from: LocalDate,
  override val to: LocalDate,
  val fromEvent: CourtAppearance,
  val toEvent: CourtAppearance,
  val chargeId: Long,
  var status: ChargeRemandStatus?,
) : Period
