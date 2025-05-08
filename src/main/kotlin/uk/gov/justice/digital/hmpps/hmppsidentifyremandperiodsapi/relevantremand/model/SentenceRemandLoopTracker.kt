package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isBeforeOrEqualTo
import java.time.LocalDate

/*
*  This class keeps track of all the variables when looping through remand periods.
*/
class SentenceRemandLoopTracker(
  private val charges: Map<Long, Charge>,
  remandPeriods: List<ChargeRemand>,
  private val sentences: List<SentenceAndCharge>,
) {
  /* All periods that are linked to a sentence which can have remand. */
  val allPeriods = remandPeriods.filter { charges[it.onlyChargeId()]!!.canHaveRemandApplyToSentence() }.sortedBy { it.from }

  /* A map of each sentence date to the periods who have a sentence with the given date */
  val sentenceDateToPeriodMap = allPeriods.groupBy { charges[it.onlyChargeId()]!!.sentenceDate!! }.toMutableMap()

  init {
    sentences.forEach {
      if (!sentenceDateToPeriodMap.containsKey(it.sentence.sentenceDate)) {
        sentenceDateToPeriodMap[it.sentence.sentenceDate] = emptyList()
      }
    }
  }

  /* A list of the periods where a sentence is being served */
  val periodsServingSentence = mutableListOf<SentencePeriod>()

  /* A list of the currently established final periods of sentence remand */
  val final = mutableListOf<Remand>()

  /* Loop variables for each sentence date iteration. */
  /* The sentence date for this iteration */
  lateinit var sentenceDate: LocalDate

  /* The loop variables needed for each loop of sentence dates */
  lateinit var periods: List<Remand>

  /* Any periods which are currently open */
  lateinit var open: MutableList<Remand>

  /* Any periods which are in the future */
  lateinit var future: MutableList<Remand>

  /* A list of dates to iterate over. Made up of any established remand or sentence periods. */
  lateinit var datesToLoopOver: List<LocalDate>

  /* Starting a new loop of the periods with the same sentence date. */
  fun startNewSentenceDateLoop(entry: Map.Entry<LocalDate, List<ChargeRemand>>) {
    sentenceDate = entry.key
    periods = entry.value.map { Remand(it.from, it.to, it.onlyChargeId()) }
    open = mutableListOf()
    future = periods.toMutableList()

    // All dates for charge remand that occur before the sentence date.
    datesToLoopOver = (periods + final).map { listOfNotNull(it.from, it.to) }.flatten().filter { it.isBefore(sentenceDate) }
    // Add any existing sentence periods
    datesToLoopOver += periodsServingSentence.flatMap { listOf(it.from, it.to) }
    // Add ending sentence date
    datesToLoopOver += sentenceDate
    // Add any recall dates for sentences on this date or before.
    datesToLoopOver += sentences.filter { it.sentence.sentenceDate.isBeforeOrEqualTo(sentenceDate) }.flatMap { it.sentence.recallDates }
    // Unique dates sorted.
    datesToLoopOver = datesToLoopOver.distinct().sorted()
  }

  /* Each date check if any periods are now closed or now open and pick which period is next. */
  fun findNextPeriod(date: LocalDate): Remand? {
    val next = future.firstOrNull()
    open.addAll(future.filter { it.from == date })
    open.removeIf { it.to == date }
    future.removeAll(open)
    return next
  }

  /* Can we open a new period, does the period intersected a confirmed date. */
  fun doesDateIntersectWithEstablishedRemandOrSentence(date: LocalDate): Boolean {
    return (final + periodsServingSentence).any { it.overlapsStartInclusive(date) } || date == sentenceDate
  }

  fun dateIsEndOfRemandOrSentence(date: LocalDate): Boolean {
    return (final + periodsServingSentence).any { it.to == date }
  }

  /* Should the current period be closed? */
  fun shouldCloseCurrentPeriod(date: LocalDate, current: Period?): Boolean {
    return ((final + periodsServingSentence).any { date == it.from } || date == sentenceDate) && current != null
  }

  /* If we've reached a sentence period then calculate the release dates for it. */
  fun shouldCalculateAReleaseDate(date: LocalDate): Boolean {
    return anyCalculationEventsOnThisDate(date) && !calculationIsForFinalSentence(date) && periodsServingSentence.none { it.from == date }
  }

  private fun anyCalculationEventsOnThisDate(date: LocalDate): Boolean {
    return sentences.any { it.sentence.sentenceDate == date || it.sentence.recallDates.any { recallDate -> recallDate == date } }
  }

  private fun finalSentence(): SentenceAndCharge {
    return sentences.maxBy { it.sentence.sentenceDate }
  }

  private fun calculationIsForFinalSentence(date: LocalDate): Boolean {
    val finalSentence = finalSentence()
    return finalSentence.sentence.sentenceDate == date || finalSentence.sentence.recallDates.any { recallDate -> recallDate == date }
  }
}
