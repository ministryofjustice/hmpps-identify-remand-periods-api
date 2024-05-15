package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.RemandDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service.CalculateReleaseDateService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceAndCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentencePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandLoopTracker

@Service
class SentenceRemandService(
  private val calculateReleaseDateService: CalculateReleaseDateService,
) {

  fun extractSentenceRemand(prisonerId: String, remandPeriods: List<ChargeRemand>, sentences: List<SentenceAndCharge>, issuesWithLegacyData: List<LegacyDataProblem>): RemandResult {
    val loopTracker = SentenceRemandLoopTracker(remandPeriods, sentences)
    for (entry in loopTracker.sentenceDateToPeriodMap.entries.sortedBy { it.key }) {
      loopTracker.startNewSentenceDateLoop(entry)
      var current: Remand? = null
      for (date in loopTracker.importantDates) {
        if (loopTracker.shouldCalculateAReleaseDate(date)) {
          val sentencesToCalculate = sentences.filter { it.sentence.sentenceDate == date || it.sentence.recallDate == date }.distinctBy { "${date}${it.sentence.bookingId}" }
          val sentenceReleaseDate = sentencesToCalculate.map { it to calculateReleaseDateService.calculateReleaseDate(prisonerId, loopTracker.final, it.sentence, date) }.maxBy { it.second }
          loopTracker.periodsServingSentence.add(SentencePeriod(date, sentenceReleaseDate.second, sentenceReleaseDate.first.sentence, sentenceReleaseDate.first.charge))
        }
        val next = loopTracker.findNextPeriod(date)
        // Should we start a new period at this date?
        if (!loopTracker.doesDateIntersectWithEstablishedRemandOrSentence(date)) {
          if (next?.from == date) {
            // New period starting from its start date.
            if (current == null) {
              current = next
            }
          }
          if (current == null && loopTracker.open.isNotEmpty()) {
            // New period starting from the end of another period.
            current = loopTracker.open.first().copy(from = date.plusDays(1))
          }
        }

        // Should the current period be closed?
        if (loopTracker.shouldCloseCurrentPeriod(date, current)) {
          // Period being closed by another period
          val end = date.minusDays(1)
          if (end.isAfter(current!!.from)) {
            loopTracker.final.add(current.copy(to = end))
          }
          current = null
        }
        if (current?.to == date) {
          // Period being closed by its end date.
          loopTracker.final.add(current)
          current = loopTracker.open.firstOrNull()?.copy(from = date.plusDays(1))
        }

        // Check if we should be opening another period immediately after closing one. (Intersecting immediate release.)
        if (!loopTracker.doesDateIntersectWithEstablishedRemandOrSentence(date)) {
          if (current == null && loopTracker.open.isNotEmpty()) {
            // New period starting from the end of another period.
            current = loopTracker.open.first().copy(from = date.plusDays(1))
          }
        }
      }
    }

    return RemandResult(
      loopTracker.final.map { toAdjustmentDto(prisonerId, it, loopTracker.allPeriods) },
      remandPeriods,
      loopTracker.final,
      loopTracker.periodsServingSentence,
      issuesWithLegacyData,
    )
  }

  private fun toAdjustmentDto(prisonerId: String, remand: Remand, allPeriods: List<ChargeRemand>): AdjustmentDto {
    return AdjustmentDto(
      id = null,
      bookingId = remand.charge.bookingId,
      sentenceSequence = remand.charge.sentenceSequence,
      fromDate = remand.from,
      toDate = remand.to,
      person = prisonerId,
      remand = RemandDto(allPeriods.filter { it.overlaps(remand) }.map { it.charge.chargeId }),
    )
  }
}
