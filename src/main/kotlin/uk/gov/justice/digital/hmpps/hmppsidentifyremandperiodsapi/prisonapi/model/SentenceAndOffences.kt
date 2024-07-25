package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.SentenceTerms
import java.math.BigDecimal
import java.time.LocalDate

data class SentenceAndOffences(
  val bookingId: Long,
  val sentenceSequence: Int,
  val sentenceDate: LocalDate,
  val sentenceStatus: String,
  val lineSequence: Int = -1,
  val caseSequence: Int = -1,
  val consecutiveToSequence: Int? = null,
  val sentenceCategory: String = "",
  val sentenceCalculationType: String = "",
  val sentenceTypeDescription: String = "",
  val terms: List<SentenceTerms> = emptyList(),
  val offences: List<OffenderOffence> = emptyList(),
  val caseReference: String? = null,
  val courtDescription: String? = null,
  val fineAmount: BigDecimal? = null,
)
