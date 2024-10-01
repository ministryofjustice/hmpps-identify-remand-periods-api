package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDateTime

data class IdentifyRemandDecisionDto(
  val accepted: Boolean,
  val rejectComment: String?,
  val options: RemandCalculationRequestOptions? = null,

  // View fields
  val days: Int? = null,
  val decisionOn: LocalDateTime? = null,
  val decisionBy: String? = null,
  val decisionByPrisonId: String? = null,
  val decisionByPrisonDescription: String? = null,
)
