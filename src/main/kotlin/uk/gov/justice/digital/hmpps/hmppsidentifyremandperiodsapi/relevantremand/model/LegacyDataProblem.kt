package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = GenericLegacyDataProblem::class)
@JsonSubTypes(
  JsonSubTypes.Type(value = ChargeLegacyDataProblem::class),
  JsonSubTypes.Type(value = GenericLegacyDataProblem::class),
)
interface LegacyDataProblem {
  val type: LegacyDataProblemType
  val message: String
  val developerMessage: String?
}
