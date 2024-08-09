package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.RemandDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandResult

@Service
class RemandAdjustmentService {

  fun getRemandedAdjustments(remandCalculation: RemandCalculation, sentenceRemand: SentenceRemandResult, chargeRemand: List<ChargeRemand>): List<AdjustmentDto> {
    return sentenceRemand.sentenceRemand.map {
      toAdjustmentDto(
        remandCalculation.prisonerId,
        it,
        chargeRemand,
        remandCalculation,
      )
    }
  }

  private fun toAdjustmentDto(
    prisonerId: String,
    remand: Remand,
    chargeRemand: List<ChargeRemand>,
    remandCalculation: RemandCalculation,
  ): AdjustmentDto {
    return AdjustmentDto(
      id = null,
      bookingId = remandCalculation.charges[remand.chargeId]!!.bookingId,
      sentenceSequence = remandCalculation.charges[remand.chargeId]!!.sentenceSequence,
      fromDate = remand.from,
      toDate = remand.to,
      person = prisonerId,
      remand = RemandDto(chargeRemand.filter { remandCalculation.charges[it.chargeIds[0]]!!.sentenceSequence != null && it.overlaps(remand) }.flatMap { it.chargeIds }),
      status = if (remandCalculation.chargeIdsWithActiveSentence.contains(remand.chargeId)) AdjustmentStatus.ACTIVE else AdjustmentStatus.INACTIVE,
    )
  }
}
