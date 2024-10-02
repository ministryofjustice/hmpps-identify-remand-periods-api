package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.RemandDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation

@Service
class RemandAdjustmentService {

  fun getRemandedAdjustments(remandCalculation: RemandCalculation, calculationData: CalculationData): List<AdjustmentDto> {
    return calculationData.sentenceRemandResult!!.sentenceRemand.map {
      toAdjustmentDto(
        remandCalculation.prisonerId,
        it,
        calculationData.chargeRemand,
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
      remand = RemandDto(chargeRemand.filter { remandCalculation.charges[it.chargeIds[0]]!!.sentenceSequence != null && it.overlaps(remand) }.flatMap { it.chargeIds }.distinct()),
      status = if (remandCalculation.chargeIdsWithActiveSentence.contains(remand.chargeId)) AdjustmentStatus.ACTIVE else AdjustmentStatus.INACTIVE,
    )
  }
}
