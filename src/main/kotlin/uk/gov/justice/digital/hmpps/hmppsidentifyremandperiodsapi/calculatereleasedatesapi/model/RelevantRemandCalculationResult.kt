package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model

import java.time.LocalDate

data class RelevantRemandCalculationResult(
  val releaseDate: LocalDate?,
  val postRecallReleaseDate: LocalDate? = null,
  val validationMessages: List<CalculateReleaseDatesValidationMessage> = emptyList(),
  val unusedDeductions: Long? = null,
)
