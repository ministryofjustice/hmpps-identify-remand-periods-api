package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

enum class ChargeRemandStatus {
  APPLICABLE,
  INACTIVE,
  INTERSECTED_BY_SENTENCE,
  INTERSECTED_BY_REMAND,
  CASE_NOT_CONCLUDED,
  NOT_SENTENCED,
  NOT_APPLICABLE_TO_TERM,
}
