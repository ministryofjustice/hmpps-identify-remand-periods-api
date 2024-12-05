package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ImprisonmentStatusType
import java.time.LocalDate

data class PrisonApiImprisonmentStatus(
  val status: String,
  val effectiveDate: LocalDate,
) {

  fun toStatusType(): ImprisonmentStatusType? {
    if (REMAND_TYPES.contains(this.status)) {
      return ImprisonmentStatusType.REMANDED
    } else if (SENTENCE_TYPES.contains(this.status)) {
      return ImprisonmentStatusType.SENTENCED
    } else if (RECALL_TYPES.contains(this.status)) {
      return ImprisonmentStatusType.RECALLED
    }
    return null
  }

  companion object {
    val REMAND_TYPES = setOf("SEC38", "TRL", "RX")
    val SENTENCE_TYPES = setOf("ADIMP_ORA20", "SENT03")
    val RECALL_TYPES = setOf("FTR/08", "LR")
  }
}
