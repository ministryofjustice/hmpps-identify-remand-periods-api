package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model

import java.time.LocalDate

data class RelevantRemandCalculationResult(
  val releaseDate: LocalDate?,
  val validationMessages: List<CalculateReleaseDatesValidationMessage> = emptyList(),
)
