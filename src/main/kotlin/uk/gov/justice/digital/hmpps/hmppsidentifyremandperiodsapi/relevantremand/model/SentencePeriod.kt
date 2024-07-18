package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class SentencePeriod(
  override val from: LocalDate,
  override val to: LocalDate,
  val sentence: Sentence,
  val chargeId: Long,
) : Period
