package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult

@Service
class RemandCalculationService(
  private val remandClockService: RemandClockService,
  private val sentenceRemandService: SentenceRemandService,
  private val remandAdjustmentService: RemandAdjustmentService,
  private val chargeRemandStatusService: ChargeRemandStatusService,
) {
  fun calculate(remandCalculation: RemandCalculation): RemandResult {
    if (remandCalculation.chargesAndEvents.isEmpty()) {
      throw UnsupportedCalculationException("There are no charges to calculate")
    }

    val chargeRemand = remandClockService.getRemandedCharges(remandCalculation)
    val sentenceRemandResult = sentenceRemandService.extractSentenceRemand(remandCalculation, chargeRemand)
    val adjustments = remandAdjustmentService.getRemandedAdjustments(remandCalculation, sentenceRemandResult, chargeRemand)

    chargeRemandStatusService.setChargeRemandStatuses(chargeRemand, adjustments, sentenceRemandResult, remandCalculation)

    return RemandResult(
      charges = remandCalculation.charges,
      adjustments = adjustments,
      chargeRemand = chargeRemand,
      sentenceRemand = sentenceRemandResult.sentenceRemand,
      intersectingSentences = sentenceRemandResult.intersectingSentences,
      intersectingSentencesUsingHistoricCalculation = sentenceRemandResult.intersectingSentencesUsingHistoricCalculation,
      issuesWithLegacyData = remandCalculation.issuesWithLegacyData,
    )
  }
}
