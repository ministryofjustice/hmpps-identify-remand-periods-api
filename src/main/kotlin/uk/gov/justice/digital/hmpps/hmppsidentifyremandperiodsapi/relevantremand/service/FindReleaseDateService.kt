package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service.CalculateReleaseDateService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.DatePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceAndCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentencePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandLoopTracker
import java.time.LocalDate

@Service
class FindReleaseDateService(
  private val historicReleaseDateService: FindHistoricReleaseDateService,
  private val calculateReleaseDateService: CalculateReleaseDateService,
  @Value("\${primary-release-date-provider:HISTORIC}") private val primaryReleaseDateService: String = "HISTORIC",
) {
  private val secondaryReleaseDateService: String = if (primaryReleaseDateService == "HISTORIC") "CRDS" else "HISTORIC"

  fun findReleaseDates(
    date: LocalDate,
    sentences: List<SentenceAndCharge>,
    loopTracker: SentenceRemandLoopTracker,
    remandCalculation: RemandCalculation,
    periodsOutOfPrison: List<DatePeriod>,
  ): SentencePeriod? {
    if (loopTracker.periodsServingSentence.any { it.from == date }) {
      return null // Already calculated this date.
    }

    val sentencesToCalculate = sentences.filter { it.sentence.sentenceDate == date || it.sentence.recallDates.any { recallDate -> recallDate == date } }.distinctBy { it.sentence.bookingId }

    val sentencePeriod = this.findReleaseDate(date, sentencesToCalculate, loopTracker, remandCalculation)

    val periodOutOfPrisonBeforeCalculatedRelease = periodsOutOfPrison.find { sentencePeriod.overlaps(it.from) }
    if (periodOutOfPrisonBeforeCalculatedRelease != null) {
      // External movement release cuts this sentence period short.
      return sentencePeriod.copy(to = periodOutOfPrisonBeforeCalculatedRelease.from, externalMovementRelease = true)
    }
    return sentencePeriod
  }

  private fun findReleaseDate(
    date: LocalDate,
    sentences: List<SentenceAndCharge>,
    loopTracker: SentenceRemandLoopTracker,
    remandCalculation: RemandCalculation,
  ): SentencePeriod {
    try {
      val calculation = getReleaseDateProvider(primaryReleaseDateService).findReleaseDate(remandCalculation.prisonerId, loopTracker.final, sentences.map { it.sentence }, date, remandCalculation.charges)
      return SentencePeriod(date, calculation.releaseDate, sentences[0].sentence, sentences[0].charge.chargeId, primaryReleaseDateService, emptyList(), calculation.calculationIds)
    } catch (primaryError: UnsupportedCalculationException) {
      try {
        val calculation = getReleaseDateProvider(secondaryReleaseDateService).findReleaseDate(
          remandCalculation.prisonerId,
          loopTracker.final,
          sentences.map { it.sentence },
          date,
          remandCalculation.charges,
        )
        return SentencePeriod(
          date,
          calculation.releaseDate,
          sentences[0].sentence,
          sentences[0].charge.chargeId,
          secondaryReleaseDateService,
          listOf(primaryError.message),
          calculation.calculationIds,
        )
      } catch (secondaryError: UnsupportedCalculationException) {
        throw UnsupportedCalculationException("Unable to calculation release dates on $date:\n ${primaryError.message} \n ${secondaryError.message}")
      }
    }
  }

  private fun getReleaseDateProvider(serviceName: String): FindReleaseDateServiceProvider = if (serviceName == "HISTORIC") {
    historicReleaseDateService
  } else {
    calculateReleaseDateService
  }
}
