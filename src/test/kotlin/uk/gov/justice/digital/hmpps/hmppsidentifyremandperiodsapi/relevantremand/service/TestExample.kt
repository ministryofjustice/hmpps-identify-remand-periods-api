package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import java.time.LocalDate

data class TestExample(
  val remandCalculation: RemandCalculation,
  val calculations: List<Calculations> = listOf(),
  val options: RemandCalculationRequestOptions = RemandCalculationRequestOptions(),
  val error: String? = null,
)

data class Calculations(
  val calculateAt: LocalDate,
  val release: LocalDate,
  val service: String = "HISTORIC",
)
