package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class ImprisonmentStatus(
  val status: ImprisonmentStatusType,
  val date: LocalDate,
  val bookNumber: String,
)

enum class ImprisonmentStatusType {
  SENTENCED,
  RECALLED,
  REMANDED,
}
