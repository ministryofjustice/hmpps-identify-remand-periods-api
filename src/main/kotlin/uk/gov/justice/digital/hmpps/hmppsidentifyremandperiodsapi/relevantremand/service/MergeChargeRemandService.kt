package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemandStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation

@Service
class MergeChargeRemandService {

  /*
    Where remand dates/events are the same and charges are within the same court case, merge the remand.
   */
  fun mergeChargeRemand(
    calculationData: CalculationData,
    remandCalculation: RemandCalculation,
  ): List<ChargeRemand> = calculationData.chargeRemand.groupBy {
    val charge = remandCalculation.charges[it.onlyChargeId()]!!
    SimilarRemandData(it.fromEvent, it.toEvent, it.status!!, charge.courtCaseRef, charge.courtLocation, charge.resultDescription, charge.sentenceSequence != null, charge.bookingId)
  }.map {
    it.value[0].copy(
      chargeIds = it.value.map { remand -> remand.onlyChargeId() }.distinct(),
      replacedCharges = it.value.mapNotNull { remand -> remand.replacedCharges.firstOrNull() },
    )
  }

  private data class SimilarRemandData(
    val fromEvent: CourtAppearance,
    val toEvent: CourtAppearance,
    val status: ChargeRemandStatus,
    val courtCaseRef: String? = null,
    val courtLocation: String? = null,
    val resultDescription: String? = null,
    val sentenced: Boolean,
    val bookingId: Long,
  )
}
