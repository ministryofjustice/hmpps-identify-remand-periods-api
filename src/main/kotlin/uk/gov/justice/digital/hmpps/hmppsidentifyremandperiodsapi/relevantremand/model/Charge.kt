package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class Charge(
  val chargeId: Long,
  val offence: Offence,
  val offenceDate: LocalDate,
  val bookingId: Long,
  val offenceEndDate: LocalDate? = null,
  val sentenceSequence: Int? = null,
  val sentenceDate: LocalDate? = null,
  val courtCaseRef: String? = null,
  val courtLocation: String? = null,
  val resultDescription: String? = null,
)
