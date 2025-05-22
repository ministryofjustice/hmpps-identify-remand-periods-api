package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class RelatedCharge(
  val offenceDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val offenceCode: String,
  val sentenceDate: LocalDate?
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RelatedCharge

    if (offenceDate != other.offenceDate) return false
    if (offenceEndDate != other.offenceEndDate) return false
    if (offenceCode != other.offenceCode) return false

    //Override the default equals here.If either sentence date is null and the offence data is the same: combine.
    //If the sentence dates are both set and are different, don't combine
    return sentenceDate == null || other.sentenceDate == null || sentenceDate == other.sentenceDate
  }

  override fun hashCode(): Int {
    var result = offenceDate.hashCode()
    result = 31 * result + (offenceEndDate?.hashCode() ?: 0)
    result = 31 * result + offenceCode.hashCode()
    result = 31 * result + (sentenceDate?.hashCode() ?: 0)
    return result
  }
}
