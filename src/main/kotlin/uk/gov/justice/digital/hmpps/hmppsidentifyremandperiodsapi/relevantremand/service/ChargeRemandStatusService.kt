package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemandStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandResult

@Service
class ChargeRemandStatusService {

  fun setChargeRemandStatuses(
    chargeRemand: List<ChargeRemand>,
    adjustments: List<AdjustmentDto>,
    sentenceRemandResult: SentenceRemandResult,
    remandCalculation: RemandCalculation,
  ) {
    chargeRemand.forEach {
      if (remandCalculation.charges[it.chargeId]!!.sentenceSequence != null) {
        val matchingAdjustments = adjustments.filter { adjustment -> adjustment.remand!!.chargeId.contains(it.chargeId) }

        if (matchingAdjustments.isNotEmpty()) {
          if (matchingAdjustments.any { adjustment -> adjustment.status == AdjustmentStatus.ACTIVE }) {
            if (matchingAdjustments.minOf { adjustment -> adjustment.remand!!.chargeId.indexOf(it.chargeId) } == 0) {
              it.status = ChargeRemandStatus.APPLICABLE
            } else {
              it.status = ChargeRemandStatus.SHARED
            }
          } else {
            it.status = ChargeRemandStatus.INACTIVE
          }
        } else {
          if (sentenceRemandResult.intersectingSentences.any { sentencePeriod -> sentencePeriod.engulfs(it) }) {
            it.status = ChargeRemandStatus.INTERSECTED
          } else {
            throw UnsupportedCalculationException("Could not determine the status of charge remand $it")
          }
        }
      } else {
        it.status = ChargeRemandStatus.NOT_YET_SENTENCED
      }
    }
  }
}
