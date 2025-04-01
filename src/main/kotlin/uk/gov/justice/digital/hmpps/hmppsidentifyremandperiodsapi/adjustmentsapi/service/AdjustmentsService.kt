package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.UnusedDeductionsCalculationResultDto
import java.util.UUID

@Service
class AdjustmentsService(
  val adjustmentsApiClient: AdjustmentsApiClient,
) {

  fun saveRemand(person: String, identified: List<AdjustmentDto>) {
    val existingAdjustments = getRemandAdjustments(person)
    val (existingDpsAdjustments, existingNomisAdjustments) = existingAdjustments.partition { it.source == "DPS" && it.remand != null }
    val create = identified.filter { new ->
      existingDpsAdjustments.none { existing -> remandSame(existing, new) }
    }
    val delete = existingDpsAdjustments
      .filter { existing ->
        identified.none { new -> remandSame(existing, new) }
        // Delete all NOMIS adjustments±±
      } + existingNomisAdjustments

    delete.forEach {
      deleteRemand(it.id!!)
    }

    if (create.isNotEmpty()) {
      createRemand(create)
    }
  }

  fun getUnusedDeductionsCalculationResult(person: String): UnusedDeductionsCalculationResultDto {
    return adjustmentsApiClient.getUnusedDeductionsCalculationResult(person)
  }

  private fun createRemand(remands: List<AdjustmentDto>) {
    adjustmentsApiClient.createAdjustment(remands)
  }

  fun getRemandAdjustments(person: String): List<AdjustmentDto> {
    return adjustmentsApiClient.getAdjustments(person).filter { it.adjustmentType == "REMAND" }
  }

  private fun deleteRemand(id: UUID) {
    adjustmentsApiClient.delete(id)
  }

  private fun remandSame(one: AdjustmentDto, two: AdjustmentDto): Boolean {
    return one.fromDate == two.fromDate && one.toDate == two.toDate && one.bookingId == two.bookingId && one.remand?.chargeId == two.remand?.chargeId
  }
}
