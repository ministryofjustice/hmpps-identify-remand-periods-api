package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceAndCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandLoopTracker
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandResult

@Service
class SentenceRemandService(
  private val findReleaseDateService: FindReleaseDateService,
) {

  fun extractSentenceRemand(remandCalculation: RemandCalculation, remandPeriods: List<ChargeRemand>): SentenceRemandResult {
    val sentences = remandCalculation.chargesAndEvents
      .filter { it.charge.sentenceDate != null && it.charge.sentenceSequence != null }
      .map { SentenceAndCharge(Sentence(it.charge.sentenceSequence!!, it.charge.sentenceDate!!, it.dates.find { date -> date.isRecallEvent }?.date, it.charge.bookingId), it.charge) }
    val loopTracker = SentenceRemandLoopTracker(remandCalculation.charges, remandPeriods, sentences)
    for (entry in loopTracker.sentenceDateToPeriodMap.entries.sortedBy { it.key }) {
      loopTracker.startNewSentenceDateLoop(entry)
      var current: Remand? = null
      for (date in loopTracker.importantDates) {
        if (loopTracker.shouldCalculateAReleaseDate(date)) {
          findReleaseDateService.findReleaseDates(date, sentences, loopTracker, remandCalculation)
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

    return SentenceRemandResult(
      loopTracker.final,
      loopTracker.periodsServingSentence,
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
