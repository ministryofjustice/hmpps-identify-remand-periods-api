package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class GenericLegacyDataProblem(
  override val type: LegacyDataProblemType,
  override val message: String,
  override val developerMessage: String? = null,
) : LegacyDataProblem
