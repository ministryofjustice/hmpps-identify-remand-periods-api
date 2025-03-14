package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.service.AdjustmentsService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemandStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ThingsToDo
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ToDoType

@Service
class ThingsToDoService(
  private val identifyRemandDecisionService: IdentifyRemandDecisionService,
  private val remandCalculationService: RemandCalculationService,
  private val adjustmentsService: AdjustmentsService,
) {

  fun getToDoList(remandCalculation: RemandCalculation): ThingsToDo {
    val prisonerId = remandCalculation.prisonerId

    val unusedDeductionsCalculationResultDto = adjustmentsService.getUnusedDeductionsCalculationResult(prisonerId)

    if (unusedDeductionsCalculationResultDto.status == "VALIDATION") {
      return ThingsToDo(prisonerId)
    }

    val decision = identifyRemandDecisionService.getDecision(prisonerId)

    val options = if (decision != null && decision.accepted) {
      decision.options!!
    } else {
      RemandCalculationRequestOptions()
    }

    val calculation = remandCalculationService.calculate(remandCalculation, options)

    val activeAdjustments = calculation.adjustments.filter { it.status == AdjustmentStatus.ACTIVE }
    val days = activeAdjustments.map { it.daysBetween() }.reduceOrNull { acc, it -> acc + it } ?: 0
    val anyPotentialUpgradeDowngrade = calculation.chargeRemand.any { listOf(ChargeRemandStatus.NOT_SENTENCED, ChargeRemandStatus.CASE_NOT_CONCLUDED).contains(it.status) }

    return if (decision == null) {
      if (days > 0) {
        ThingsToDo(prisonerId, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_FIRST_TIME), days)
      } else if (anyPotentialUpgradeDowngrade) {
        ThingsToDo(prisonerId, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_FIRST_TIME_UPGRADE_DOWNGRADE), days)
      } else {
        ThingsToDo(prisonerId)
      }
    } else {
      if (days == decision.days) {
        if (decision.accepted) {
          val adjustments = adjustmentsService.getRemandAdjustments(prisonerId)
          val persistedDays = adjustments.map { it.days ?: it.daysBetween() }.reduceOrNull { acc, it -> acc + it } ?: 0
          if (persistedDays != days) {
            ThingsToDo(prisonerId, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_UPDATE), days)
          } else {
            ThingsToDo(prisonerId)
          }
        } else {
          ThingsToDo(prisonerId)
        }
      } else {
        ThingsToDo(prisonerId, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_UPDATE), days)
      }
    }
  }
}
