package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeLegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.DatePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.GenericLegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ImprisonmentStatusType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isBeforeOrEqualTo
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.mojDisplayFormat

@Service
class ValidateCalculationDataService {

  fun validate(calculationData: CalculationData) {
    validateOffenceDates(calculationData)
    validateRecallEventForEachRecallSentence(calculationData)
    validateRecallEventNotOnSentenceDate(calculationData)
    validateImprisonmentStatuses(calculationData)
    validateUnclosedRemandDatesWithSentence(calculationData)
    validateAllStartEventsBeforeSentenceDate(calculationData)
  }

  private fun validateAllStartEventsBeforeSentenceDate(calculationData: CalculationData) {
    calculationData.chargeAndEvents.mapNotNull {
      if (it.charge.sentenceDate != null) {
        val eventAfterSentenceDate = it.dates.find { courtEvent -> courtEvent.type.shouldStartRemand() && courtEvent.date == it.charge.sentenceDate }
        if (eventAfterSentenceDate != null) it to eventAfterSentenceDate else null
      } else {
        null
      }
    }.forEach {
      val charge = it.first.charge
      val event = it.second
      calculationData.issuesWithLegacyData.add(
        ChargeLegacyDataProblem(
          LegacyDataProblemType.START_EVENT_ON_SENTENCE_DATE,
          "The offence '${charge.offence.description}' within booking ${charge.bookNumber} has a court event which indicates the start of remand '${event.description}' on the same date as the sentence date ${charge.sentenceDate!!.mojDisplayFormat()}",
          charge,
        ),
      )
    }
  }

  private fun validateUnclosedRemandDatesWithSentence(calculationData: CalculationData) {
    calculationData.unclosedRemandDates
      .filter { it.charge.sentenceDate != null }
      .forEach {
        calculationData.issuesWithLegacyData.add(
          ChargeLegacyDataProblem(
            LegacyDataProblemType.MISSING_STOP_EVENT,
            "The offence '${it.charge.offence.description}' within booking ${it.charge.bookNumber} has no court event to stop a remand period starting on ${it.start.mojDisplayFormat()}",
            it.charge,
          ),
        )
      }
  }

  private fun validateImprisonmentStatuses(calculationData: CalculationData) {
    val maximumCalculatedDate = (calculationData.sentenceRemandResult!!.sentenceRemand + calculationData.sentenceRemandResult!!.intersectingSentences).maxOfOrNull { it.to }
    calculationData.imprisonmentStatuses
      .filter { maximumCalculatedDate == null || it.date.isBeforeOrEqualTo(maximumCalculatedDate) }
      .forEach {
        when (it.status) {
          ImprisonmentStatusType.RECALLED -> validateRecalledImprisonmentStatus(it, calculationData)
          ImprisonmentStatusType.REMANDED -> validateRemandedImprisonmentStatus(it, calculationData)
          else -> return@forEach
        }
      }
  }

  /**
   * Validate that the imprisonment status change happens within a remand period, or on a court date. (Remand statuses are often used by reception)
   */
  private fun validateRemandedImprisonmentStatus(status: ImprisonmentStatus, calculationData: CalculationData) {
    val anyMatchingRemandPeriod = calculationData.chargeRemand.any { it.overlapsStartAndEndInclusive(status.date) }
    val anyMatchingCourtEvent = calculationData.chargeAndEvents.flatMap { it.dates }.any { it.date == status.date }
    val anyOpenRemand = calculationData.unclosedRemandDates.any { it.start.isBeforeOrEqualTo(status.date) }
    if (!anyMatchingRemandPeriod && !anyMatchingCourtEvent && !anyOpenRemand) {
      calculationData.issuesWithLegacyData.add(
        GenericLegacyDataProblem(
          LegacyDataProblemType.MISSING_COURT_EVENT_FOR_IMPRISONMENT_STATUS_REMAND,
          "The offenders main inmate status was changed to remanded on ${status.date.mojDisplayFormat()} but there is no matching court events",
        ),
      )
    }
  }

  /**
   * Check if recall status happens within active adjustment.
   */
  private fun validateRecalledImprisonmentStatus(status: ImprisonmentStatus, calculationData: CalculationData) {
    val recalledDuringActiveRemandPeriod = calculationData.adjustments
      .filter { it.status == AdjustmentStatus.ACTIVE }
      .any { DatePeriod(it.fromDate!!, it.toDate!!).overlapsStartAndEndInclusive(status.date) }
    if (recalledDuringActiveRemandPeriod) {
      calculationData.issuesWithLegacyData.add(
        GenericLegacyDataProblem(
          LegacyDataProblemType.MISSING_COURT_EVENT_FOR_IMPRISONMENT_STATUS_RECALL,
          "The offenders main inmate status was changed to recalled on ${status.date.mojDisplayFormat()} but there is no matching court events",

        ),
      )
    }
  }

  private fun validateOffenceDates(calculationData: CalculationData) {
    calculationData.chargeAndEvents.forEach {
      val charge = it.charge
      if (charge.offenceDate == null) {
        calculationData.issuesWithLegacyData.add(
          ChargeLegacyDataProblem(
            LegacyDataProblemType.MISSING_OFFENCE_DATE,
            "There is another offence of '${charge.offence.description}' within booking ${charge.bookNumber} that has a missing offence date.",
            charge,
          ),
        )
      }
    }
  }

  private fun validateRecallEventForEachRecallSentence(calculationData: CalculationData) {
    val recalledCharges = calculationData.chargeAndEvents.filter { it.charge.isRecallSentence }
    val missingRecallEvents = recalledCharges.filter { it.dates.none { event -> event.isRecallEvent } }

    missingRecallEvents.forEach {
      calculationData.issuesWithLegacyData.add(ChargeLegacyDataProblem(LegacyDataProblemType.MISSING_RECALL_EVENT, "The offence '${it.charge.offence.description}' within booking ${it.charge.bookNumber} does not have a recall court event.", it.charge))
    }
  }
  private fun validateRecallEventNotOnSentenceDate(calculationData: CalculationData) {
    val (recallEventOnSentenceDateCharges, chargeAndEvents) = calculationData.chargeAndEvents.partition { it.charge.sentenceDate != null && it.dates.any { event -> event.isRecallEvent && event.date.isBeforeOrEqualTo(it.charge.sentenceDate) } }

    recallEventOnSentenceDateCharges.forEach {
      calculationData.issuesWithLegacyData.add(ChargeLegacyDataProblem(LegacyDataProblemType.RECALL_EVENT_ON_SENTENCE_DATE, "The offence '${it.charge.offence.description}' within booking ${it.charge.bookNumber} has a recall event before or on sentence date", it.charge))
    }
    calculationData.chargeAndEvents = chargeAndEvents
  }
}
