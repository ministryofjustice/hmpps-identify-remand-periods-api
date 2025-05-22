package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class RelatedCharge(
  val offenceDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val offenceCode: String,
  val sentenceDate: LocalDate?,
) {

  /*
   * Charges are the same if they have all the same offence details (date and code), and they were sentenced at the same time.
   * However a charge may not be sentenced until its recreated on another booking. So they're only unrelated if both sentence dates
   * are non null and different.
   */
  fun isRelated(other: RelatedCharge): Boolean {
    if (offenceDate != other.offenceDate) return false
    if (offenceEndDate != other.offenceEndDate) return false
    if (offenceCode != other.offenceCode) return false

    return sentenceDate == null || other.sentenceDate == null || sentenceDate == other.sentenceDate
  }
}
