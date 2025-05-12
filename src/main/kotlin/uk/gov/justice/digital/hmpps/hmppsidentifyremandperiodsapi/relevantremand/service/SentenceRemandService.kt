package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.DatePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ExternalMovement
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceAndCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandLoopTracker
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isAfterOrEqualTo
import java.time.LocalDate

@Service
class SentenceRemandService(
  private val findReleaseDateService: FindReleaseDateService,
) {

  fun extractSentenceRemand(remandCalculation: RemandCalculation, calculationData: CalculationData): SentenceRemandResult {
    val periodsOutOfPrison = findPeriodsOutOfPrison(remandCalculation.externalMovements)
    val sentences = calculationData.chargeAndEvents
      .filter { it.charge.sentenceDate != null && it.charge.sentenceSequence != null }
      .flatMap {
        val similarCharges = it.relatedCharges.map { chargeId -> remandCalculation.charges[chargeId]!! }.filter { charge -> charge.sentenceDate != null && charge.sentenceSequence != null }
        val charges = (similarCharges + it.charge).distinctBy { charge -> charge.bookingId }
        charges.map { charge ->
          SentenceAndCharge(Sentence(charge.sentenceSequence!!, charge.sentenceDate!!, it.dates.filter { date -> date.isRecallEvent }.map { date -> date.date }.sorted(), charge.bookingId), charge)
        }
      }.distinctBy { "${it.sentence.sentenceDate}${it.sentence.recallDates}${it.sentence.bookingId}" }
    val loopTracker = SentenceRemandLoopTracker(remandCalculation.charges, calculationData.chargeRemand, sentences, periodsOutOfPrison)
    for (entry in loopTracker.sentenceDateToPeriodMap.entries.sortedBy { it.key }) {
      loopTracker.startNewSentenceDateLoop(entry)
      var current: Remand? = null
      for (date in loopTracker.datesToLoopOver) {
        if (loopTracker.shouldCalculateAReleaseDate(date)) {
          val period = findReleaseDateService.findReleaseDates(date, sentences, loopTracker, remandCalculation, periodsOutOfPrison)
          if (period != null) {
            loopTracker.periodsServingSentence.add(period)
          }
        }
        val next = loopTracker.findNextPeriod(date)
        // Should we start a new period at this date?
        if (!loopTracker.doesDateIntersectWithEstablishedRemandOrSentence(date)) {
          if (loopTracker.dateIsEndOfRemandOrSentence(date)) {
            if (current == null && loopTracker.open.isNotEmpty()) {
              // New period starting from the end of another period.
              current = loopTracker.open.first().copy(from = date.plusDays(1))
            }
          } else {
            if (next?.from == date) {
              // New period starting from its start date.
              if (current == null) {
                current = next
              }
            }
          }
        }

        // Should the current period be closed?
        if (loopTracker.shouldCloseCurrentPeriod(date, current)) {
          // Period being closed by another period
          val end = date.minusDays(1)
          if (end.isAfterOrEqualTo(current!!.from)) {
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
      periodsOutOfPrison,
    )
  }

  private fun findPeriodsOutOfPrison(externalMovements: List<ExternalMovement>): List<DatePeriod> {
    var outsideStart: LocalDate? = null
    val periodsOutOfPrison = mutableListOf<DatePeriod>()
    externalMovements.forEach {
      if (it.release) {
        outsideStart = it.date
      } else {
        if (outsideStart != null) {
          //The period out of prison ends the day before they're admitted into prison.
          periodsOutOfPrison.add(DatePeriod(outsideStart!!, it.date.minusDays(1)))
          outsideStart = null
        }
      }
    }
    return periodsOutOfPrison
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
