package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult

@Service
class RemandCalculationService(
  private val relatedChargeCombinationService: RelatedChargeCombinationService,
  private val userSelectedCombinationService: UserSelectedCombinationService,
  private val remandClockService: RemandClockService,
  private val sentenceRemandService: SentenceRemandService,
  private val remandAdjustmentService: RemandAdjustmentService,
  private val chargeRemandStatusService: ChargeRemandStatusService,
  private val resultSortingService: ResultSortingService,
  private val mergeChargeRemandService: MergeChargeRemandService,
  private val validateCalculationDataService: ValidateCalculationDataService,
) {

  fun calculate(remandCalculation: RemandCalculation, options: RemandCalculationRequestOptions): RemandResult {
    if (remandCalculation.chargesAndEvents.isEmpty()) {
      throw UnsupportedCalculationException("There are no charges to calculate")
    }

    val calculationData = CalculationData(issuesWithLegacyData = remandCalculation.issuesWithLegacyData.toMutableList(), imprisonmentStatuses = remandCalculation.imprisonmentStatuses)

    calculationData.chargeAndEvents = relatedChargeCombinationService.combineRelatedCharges(remandCalculation)

    val remandClockResult = remandClockService.remandClock(calculationData)
    calculationData.chargeRemand = remandClockResult.chargeRemand
    calculationData.unclosedRemandDates = remandClockResult.unclosedRemandDates

    userSelectedCombinationService.combineUserSelectedCharges(calculationData, options)

    calculationData.sentenceRemandResult = sentenceRemandService.extractSentenceRemand(remandCalculation, calculationData)
    calculationData.adjustments = remandAdjustmentService.getRemandedAdjustments(remandCalculation, calculationData)

    calculationData.chargeRemand = chargeRemandStatusService.setChargeRemandStatuses(calculationData, remandCalculation)
    calculationData.chargeRemand = mergeChargeRemandService.mergeChargeRemand(calculationData, remandCalculation)

    validateCalculationDataService.validate(calculationData)

    val unsortedResult = RemandResult(
      charges = remandCalculation.charges,
      adjustments = calculationData.adjustments,
      chargeRemand = calculationData.chargeRemand,
      intersectingSentences = calculationData.sentenceRemandResult!!.intersectingSentences,
      issuesWithLegacyData = calculationData.issuesWithLegacyData.distinctBy { it.message },
      periodsOutOfPrison = calculationData.sentenceRemandResult!!.periodsOutOfPrison,
      remandCalculation = if (options.includeRemandCalculation) remandCalculation else null,
    )

    return resultSortingService.sort(unsortedResult)
  }
}
