package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isBeforeOrEqualTo

@Service
class ValidateChargeService {

  fun validate(calculationData: CalculationData) {
    validateRecallEventForEachRecallSentence(calculationData)
    validateRecallEventNotOnSentenceDate(calculationData)
  }

  private fun validateRecallEventForEachRecallSentence(calculationData: CalculationData) {
    val recalledCharges = calculationData.chargeAndEvents.filter { it.charge.isRecallSentence }
    val missingRecallEvents = recalledCharges.filter { it.dates.none { event -> event.isRecallEvent } }

    missingRecallEvents.forEach {
      calculationData.issuesWithLegacyData.add(LegacyDataProblem(LegacyDataProblemType.MISSING_RECALL_EVENT, "The offence '${it.charge.offence.description}' within booking ${it.charge.bookNumber} does not have a recall court event.", it.charge))
    }
  }
  private fun validateRecallEventNotOnSentenceDate(calculationData: CalculationData) {
    val recallEventOnSentenceDateCharges = calculationData.chargeAndEvents.filter { it.charge.sentenceDate != null && it.dates.any { event -> event.isRecallEvent && event.date.isBeforeOrEqualTo(it.charge.sentenceDate) } }

    recallEventOnSentenceDateCharges.forEach {
      calculationData.issuesWithLegacyData.add(LegacyDataProblem(LegacyDataProblemType.RECALL_EVENT_ON_SENTENCE_DATE, "The offence '${it.charge.offence.description}' within booking ${it.charge.bookNumber} has a recall event before or on sentence date", it.charge))
    }
    calculationData.chargeAndEvents = calculationData.chargeAndEvents.filter { !recallEventOnSentenceDateCharges.contains(it) }
  }
}
