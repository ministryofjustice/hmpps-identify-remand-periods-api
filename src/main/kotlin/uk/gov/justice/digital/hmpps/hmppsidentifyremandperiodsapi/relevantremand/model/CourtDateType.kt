package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

enum class CourtDateType {
  START,
  STOP,
  CONTINUE,
  ;

  fun shouldStartRemand() = this == START || this == CONTINUE
}
