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
  val resultCode: String?,
) {
  fun canHaveRemandApplyToSentence(): Boolean = sentenceDate != null && sentenceSequence != null && !isTermSentence

  fun isInConclusive() = resultCode != null && INCONCLUSIVE_RESULT_CODES.contains(resultCode)

  companion object{
    val INCONCLUSIVE_RESULT_CODES = setOf(
      "1004", "1008", "1009", "1010", "1012", "1013", "1018", "1028",
      "1089", "1102", "1108", "1113", "1115", "1116",
      "1507", "1508",
      "2008", "2009", "2060", "2061", "2067", "2068",
      "2501",
      "3006", "3019", "3047", "3063", "3067", "3112",
      "3501", "3502",
      "4011", "4013", "4014", "4015", "4017", "4022",
      "4529", "4530", "4533", "4538", "4542", "4543", "4548", "4550", "4552", "4555", "4558", "4559", "4562",
      "4572", "4575", "4577", "4582", "4587", "4589",
      "FPR", "NC",
    )
  }
}
