package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model

import java.time.LocalDate

data class RelevantRemandCalculationResult(
  val releaseDate: LocalDate?,
  val postRecallReleaseDate: LocalDate? = null,
  val unusedDeductions: Int = 0,
  val validationMessages: List<CalculateReleaseDatesValidationMessage> = emptyList(),
)
