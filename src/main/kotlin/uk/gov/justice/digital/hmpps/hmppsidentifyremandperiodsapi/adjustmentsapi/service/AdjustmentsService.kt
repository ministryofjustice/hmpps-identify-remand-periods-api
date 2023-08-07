package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import java.util.UUID

@Service
class AdjustmentsService(
  val adjustmentsApiClient: AdjustmentsApiClient
) {

  fun saveRemand(person: String, remand: List<AdjustmentDto>) {
    val existingAdjustments = getAdjustments(person)
    val create = remand.filter {new ->
      existingAdjustments.none { existing -> remandSame(existing, new) }
    }
    val delete = existingAdjustments.filter { existing ->
      remand.none { new -> remandSame(existing, new) }
    }

    delete.forEach {
      deleteRemand(it.id!!)
    }

    create.forEach {
      saveRemand(it)
    }
  }

  private fun saveRemand(remand: AdjustmentDto) {
    adjustmentsApiClient.createAdjustment(remand)
  }

  private fun getAdjustments(person: String): List<AdjustmentDto> {
    return adjustmentsApiClient.getAdjustments(person).filter { it.adjustmentType == "REMAND" }
  }

  private fun deleteRemand(id: UUID) {
    adjustmentsApiClient.delete(id)
  }

  private fun remandSame(one: AdjustmentDto, two: AdjustmentDto): Boolean {
    return one.fromDate == two.fromDate && one.toDate == two.toDate && one.bookingId == two.bookingId && one.sentenceSequence == two.sentenceSequence
  }
}