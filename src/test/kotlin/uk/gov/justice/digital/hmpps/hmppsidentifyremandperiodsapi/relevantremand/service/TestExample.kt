package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import java.time.LocalDate

data class TestExample(
  val remandCalculation: RemandCalculation,
  val sentences: List<Sentences> = listOf(),
  val options: RemandCalculationRequestOptions = RemandCalculationRequestOptions(),
)

data class Sentences(
  val bookingId: Long,
  val sentenceSequence: Int,
  val sentenceAt: LocalDate,
  val recallDates: List<LocalDate> = emptyList(),
  val calculations: MutableList<Calculations>,
)

data class Calculations(
  val calculateAt: LocalDate,
  val release: LocalDate,
  val service: String = "HISTORIC",
)
