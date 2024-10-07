package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.RemandDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.DatePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import java.time.LocalDate

@Service
class RemandAdjustmentService {

  fun getRemandedAdjustments(remandCalculation: RemandCalculation, calculationData: CalculationData): List<AdjustmentDto> {
    return calculationData.sentenceRemandResult!!.sentenceRemand.map {
      toAdjustmentDto(
        remandCalculation,
        calculationData,
        it,
      )
    }
  }

  private fun toAdjustmentDto(remandCalculation: RemandCalculation, calculationData: CalculationData, remand: Remand): AdjustmentDto {
    val sentenceDate = remandCalculation.charges[remand.chargeId]!!.sentenceDate!!
    val endOfSentencePeriod = findEndOfSentencePeriod(sentenceDate, calculationData)
    val periodOfConcurrentSentences = DatePeriod(sentenceDate, endOfSentencePeriod)

    val charges = calculationData.chargeRemand.filter {
      val itSentenceDate = remandCalculation.charges[it.onlyChargeId()]!!.sentenceDate
      itSentenceDate != null && (periodOfConcurrentSentences.overlapsStartInclusive(itSentenceDate) || periodOfConcurrentSentences.from == itSentenceDate)
    }.flatMap { it.chargeIds }.distinct()
    return AdjustmentDto(
      id = null,
      bookingId = remandCalculation.charges[remand.chargeId]!!.bookingId,
      sentenceSequence = remandCalculation.charges[remand.chargeId]!!.sentenceSequence,
      fromDate = remand.from,
      toDate = remand.to,
      person = remandCalculation.prisonerId,
      remand = RemandDto(charges),
      status = if (charges.any { remandCalculation.chargeIdsWithActiveSentence.contains(it) }) AdjustmentStatus.ACTIVE else AdjustmentStatus.INACTIVE,
    )
  }

  private fun findEndOfSentencePeriod(sentenceDate: LocalDate, calculationData: CalculationData): LocalDate {
    val intersectingSentences = calculationData.sentenceRemandResult!!.intersectingSentences
    if (intersectingSentences.any { it.from == sentenceDate }) {
      var currentEnd = sentenceDate
      while (intersectingSentences.any { it.overlapsStartInclusive(currentEnd) }) {
        currentEnd = intersectingSentences.filter { it.overlapsStartInclusive(currentEnd) }.maxOf { it.to }
      }
      return currentEnd
    }
    return sentenceDate
  }
}
