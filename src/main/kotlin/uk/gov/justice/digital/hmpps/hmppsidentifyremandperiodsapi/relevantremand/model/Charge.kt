package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class Charge(
  val chargeId: Long,
  val offence: Offence,
  val bookingId: Long,
  val offenceDate: LocalDate? = null,
  val bookNumber: String = "",
  val offenceEndDate: LocalDate? = null,
  val sentenceSequence: Int? = null,
  val sentenceDate: LocalDate? = null,
  val courtCaseRef: String? = null,
  val courtLocation: String? = null,
  val resultDescription: String? = null,
  val final: Boolean = false,
  val isActiveBooking: Boolean = false,
  val isRecallSentence: Boolean = false,
  val isTermSentence: Boolean = false,
) {
  fun canHaveRemandApplyToSentence(): Boolean = sentenceDate != null && sentenceSequence != null && !isTermSentence
}
