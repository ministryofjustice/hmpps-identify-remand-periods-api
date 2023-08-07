package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDateTime

data class IdentifyRemandDecisionDto(
  val accepted: Boolean,
  val rejectComment: String?,


  //View fields
  val days: Int? = null,
  val decisionOn: LocalDateTime? = null,
  val decisionBy: String? = null
)
