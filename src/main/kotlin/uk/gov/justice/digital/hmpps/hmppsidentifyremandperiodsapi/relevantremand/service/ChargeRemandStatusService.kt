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
  ): List<ChargeRemand> {
    return chargeRemand.map {
      val status = if (remandCalculation.charges[it.onlyChargeId()]!!.sentenceSequence != null) {
        val matchingAdjustments = adjustments.filter { adjustment -> adjustment.remand!!.chargeId.contains(it.onlyChargeId()) }

        if (matchingAdjustments.isNotEmpty()) {
          if (matchingAdjustments.any { adjustment -> adjustment.status == AdjustmentStatus.ACTIVE }) {
            if (matchingAdjustments.minOf { adjustment -> adjustment.remand!!.chargeId.indexOf(it.onlyChargeId()) } == 0) {
              ChargeRemandStatus.APPLICABLE
            } else {
              ChargeRemandStatus.SHARED
            }
          } else {
            ChargeRemandStatus.INACTIVE
          }
        } else {
          if (sentenceRemandResult.intersectingSentences.any { sentencePeriod -> sentencePeriod.engulfs(it) }) {
            ChargeRemandStatus.INTERSECTED
          } else {
            throw UnsupportedCalculationException("Could not determine the status of charge remand $it")
          }
        }
      } else {
        val charge = remandCalculation.charges[it.onlyChargeId()]!!
        if (charge.isFinal) {
          ChargeRemandStatus.NOT_SENTENCED
        } else {
          ChargeRemandStatus.CASE_NOT_CONCLUDED
        }

      }
      it.copy(
        status = status,
      )
    }
  }
}
