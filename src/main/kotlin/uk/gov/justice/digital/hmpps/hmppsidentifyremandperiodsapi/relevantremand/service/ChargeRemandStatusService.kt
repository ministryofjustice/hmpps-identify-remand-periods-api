package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemandStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.DatePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation

@Service
class ChargeRemandStatusService {

  fun setChargeRemandStatuses(
    calculationData: CalculationData,
    adjustments: List<AdjustmentDto>,
    remandCalculation: RemandCalculation,
  ): List<ChargeRemand> {
    return calculationData.chargeRemand.map {
      val status = if (remandCalculation.charges[it.onlyChargeId()]!!.sentenceSequence != null) {
        val matchingChargeId = adjustments.filter { adjustment -> adjustment.remand!!.chargeId.contains(it.onlyChargeId()) }
        val matchingAdjustments = matchingChargeId.filter { adjustment -> DatePeriod(adjustment.fromDate!!, adjustment.toDate!!).overlaps(it) }

        if (matchingAdjustments.isNotEmpty()) {
          if (matchingAdjustments.any { adjustment -> adjustment.status == AdjustmentStatus.ACTIVE }) {
            ChargeRemandStatus.APPLICABLE
          } else {
            ChargeRemandStatus.INACTIVE
          }
        } else {
          if (calculationData.sentenceRemandResult!!.intersectingSentences.any { sentencePeriod -> sentencePeriod.engulfs(it) }) {
            ChargeRemandStatus.INTERSECTED_BY_SENTENCE
          } else if (matchingChargeId.isNotEmpty()) {
            ChargeRemandStatus.INTERSECTED_BY_REMAND
          } else {
            throw UnsupportedCalculationException("Could not determine the status of charge remand $it")
          }
        }
      } else {
        val charge = remandCalculation.charges[it.onlyChargeId()]!!
        if (charge.final) {
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
