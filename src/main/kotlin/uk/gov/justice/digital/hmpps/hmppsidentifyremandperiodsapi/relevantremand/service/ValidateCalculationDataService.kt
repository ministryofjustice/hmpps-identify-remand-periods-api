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
import java.time.format.DateTimeFormatter

@Service
class ValidateCalculationDataService {

  fun validate(calculationData: CalculationData) {
    validateOffenceDates(calculationData)
    validateRecallEventForEachRecallSentence(calculationData)
    validateRecallEventNotOnSentenceDate(calculationData)
    validateImprisonmentStatuses(calculationData)
  }

  private fun validateImprisonmentStatuses(calculationData: CalculationData) {
    val maximumCalculatedDate = (calculationData.sentenceRemandResult!!.sentenceRemand + calculationData.sentenceRemandResult!!.intersectingSentences).maxOfOrNull { it.to }
    calculationData.imprisonmentStatuses
      .filter { maximumCalculatedDate == null || it.date.isBeforeOrEqualTo(maximumCalculatedDate) }
      .forEach {
        when (it.status) {
          ImprisonmentStatusType.SENTENCED -> validateSentencedImprisonmentStatus(it, calculationData)
          ImprisonmentStatusType.RECALLED -> validateRecalledImprisonmentStatus(it, calculationData)
          ImprisonmentStatusType.REMANDED -> validateRemandedImprisonmentStatus(it, calculationData)
        }
      }
  }

  private fun validateRemandedImprisonmentStatus(status: ImprisonmentStatus, calculationData: CalculationData) {
    val anyMatchingCourtEvent = calculationData.chargeRemand.any { it.overlapsStartAndEndInclusive(status.date) }
    val statuses = calculationData.imprisonmentStatuses
    var nextStatusIsDayAfter = false // Remand status is often set when prisoner returns from court, even if they've been sentenced. The next day that status is replaced.
    if (statuses.last() != status) {
      val nextStatus = statuses[statuses.indexOf(status) + 1]
      nextStatusIsDayAfter = nextStatus.date == status.date.plusDays(1)
    }
    if (!anyMatchingCourtEvent && !nextStatusIsDayAfter) {
      calculationData.issuesWithLegacyData.add(
        GenericLegacyDataProblem(
          LegacyDataProblemType.MISSING_COURT_EVENT_FOR_IMPRISONMENT_STATUS_REMAND,
          "The offenders main inmate status was changed to remanded on ${status.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} but there is no matching court events",
        ),
      )
    }
  }

  private fun validateRecalledImprisonmentStatus(status: ImprisonmentStatus, calculationData: CalculationData) {
    val recalledDuringActiveRemandPeriod = calculationData.adjustments
      .filter { it.status == AdjustmentStatus.ACTIVE }
      .any { DatePeriod(it.fromDate!!, it.toDate!!).overlapsStartAndEndInclusive(status.date) }
    if (recalledDuringActiveRemandPeriod) {
      calculationData.issuesWithLegacyData.add(
        GenericLegacyDataProblem(
          LegacyDataProblemType.MISSING_COURT_EVENT_FOR_IMPRISONMENT_STATUS_RECALL,
          "The offenders main inmate status was changed to recalled on ${status.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} but there is no matching court events",

        ),
      )
    }
  }

  private fun validateSentencedImprisonmentStatus(status: ImprisonmentStatus, calculationData: CalculationData) {
    val anyMatchingIntersectingSentence = calculationData.sentenceRemandResult!!.intersectingSentences.any { it.overlapsStartAndEndInclusive(status.date) }
    val anyMatchingSentenceDate = calculationData.chargeAndEvents.any { it.charge.sentenceDate == status.date || it.charge.sentenceDate == status.date.minusDays(1) } // Imprisonment status is often set to day after sentencing
    if (!anyMatchingIntersectingSentence && !anyMatchingSentenceDate) {
      calculationData.issuesWithLegacyData.add(
        GenericLegacyDataProblem(
          LegacyDataProblemType.MISSING_COURT_EVENT_FOR_IMPRISONMENT_STATUS_SENTENCING,
          "The offenders main inmate status was changed to sentenced on ${status.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} but there is no sentence",
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
