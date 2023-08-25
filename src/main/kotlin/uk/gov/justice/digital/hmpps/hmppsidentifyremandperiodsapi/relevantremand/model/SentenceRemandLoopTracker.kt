package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

/*
*  This class keeps track of all the variables when looping through remand periods.
*/
class SentenceRemandLoopTracker(
  remandPeriods: List<ChargeRemand>,
  private val sentences: List<SentenceAndCharge>,
) {
  /* All periods that are linked to a sentence */
  private val allPeriods = remandPeriods.filter { it.charge.sentenceSequence != null && it.charge.sentenceDate != null }

  /* A map of each sentence date to the periods who have a sentence with the given date */
  val sentenceDateToPeriodMap = allPeriods.groupBy { it.charge.sentenceDate }.toMutableMap()

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

  /* The loop variables needed for each loop of sentence dates */
  lateinit var periods: List<Remand>

  /* Any periods which are currently open */
  lateinit var open: MutableList<Remand>

  /* Any periods which are in the future */
  lateinit var future: MutableList<Remand>

  /* A list of dates to iterate over. Made up of any established remand or sentence periods. */
  lateinit var importantDates: List<LocalDate>

  /* Starting a new loop of the periods with the same sentence date. */
  fun startNewSentenceDateLoop(entry: Map.Entry<LocalDate?, List<ChargeRemand>>) {
    periods = entry.value.map { Remand(it.from, it.to, it.charge) }
    open = mutableListOf()
    future = periods.toMutableList()
    importantDates = ((periods + final).map { listOfNotNull(it.from, it.to, it.charge.sentenceDate) }.flatten() + listOfNotNull(entry.key) + periodsServingSentence.flatMap { listOf(it.from, it.to) } + sentences.mapNotNull { it.sentence.recallDate }).distinct().sorted()
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
    return !(!sentences.any { it.sentence.sentenceDate == date } && (final + periodsServingSentence).none { it.overlapsStartInclusive(date.plusDays(1)) })
  }

  /* Should the current period be closed? */
  fun shouldCloseCurrentPeriod(date: LocalDate, current: Period?): Boolean {
    return (final + periodsServingSentence).any { date == it.from } && current != null
  }

  /* If we've reached a sentence period then calculate the release dates for it. */
  fun shouldCalculateAReleaseDate(date: LocalDate): Boolean {
    return sentences.any { it.sentence.sentenceDate == date || it.sentence.recallDate == date } && sentences.maxOf { it.sentence.sentenceDate } != date && periodsServingSentence.none { it.from == date }
  }
}
