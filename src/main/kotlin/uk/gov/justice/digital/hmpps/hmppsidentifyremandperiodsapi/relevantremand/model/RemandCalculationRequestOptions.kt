package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class RemandCalculationRequestOptions(
  // Should the output from the calculation include the 'RemandCalculation' source object.
  val includeRemandCalculation: Boolean = false,

  val userSelections: List<RemandApplicableUserSelection> = emptyList(),
)
