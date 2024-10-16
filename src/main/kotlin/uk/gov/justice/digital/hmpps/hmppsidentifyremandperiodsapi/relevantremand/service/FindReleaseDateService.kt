package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service.CalculateReleaseDateService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
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
  ) {
    if (loopTracker.periodsServingSentence.any { it.from == date }) {
      return // Already calculated this date.
    }

    val sentencesToCalculate = sentences.filter { it.sentence.sentenceDate == date || it.sentence.recallDates.any { recallDate -> recallDate == date } }.distinctBy { it.sentence.bookingId }

    val periods = sentencesToCalculate.map {
      this.findReleaseDate(date, it, loopTracker, remandCalculation)
    }

    if (periods.none { it.first != null }) {
      val errors = periods.first().second!!
      throw UnsupportedCalculationException("Unable to calculation release dates on $date:\n ${errors.joinToString("\n") {it.message} }")
    }

    loopTracker.periodsServingSentence.addAll(
      periods.mapNotNull { it.first },
    )
  }

  private fun findReleaseDate(
    date: LocalDate,
    sentence: SentenceAndCharge,
    loopTracker: SentenceRemandLoopTracker,
    remandCalculation: RemandCalculation,
  ): Pair<SentencePeriod?, List<UnsupportedCalculationException>?> {
    try {
      val calculation = getReleaseDateProvider(primaryReleaseDateService).findReleaseDate(remandCalculation.prisonerId, loopTracker.final, sentence.sentence, date, remandCalculation.charges, loopTracker.periodsServingSentence)
      return SentencePeriod(date, calculation.releaseDate, sentence.sentence, sentence.charge.chargeId, primaryReleaseDateService, emptyList(), calculation.calculationIds) to null
    } catch (primaryError: UnsupportedCalculationException) {
      try {
        val calculation = getReleaseDateProvider(secondaryReleaseDateService).findReleaseDate(
          remandCalculation.prisonerId,
          loopTracker.final,
          sentence.sentence,
          date,
          remandCalculation.charges,
          loopTracker.periodsServingSentence,
        )
        return SentencePeriod(
          date,
          calculation.releaseDate,
          sentence.sentence,
          sentence.charge.chargeId,
          secondaryReleaseDateService,
          listOf(primaryError.message),
          calculation.calculationIds,
        ) to null
      } catch (secondaryError: UnsupportedCalculationException) {
        return null to listOf(primaryError, secondaryError)
      }
    }
  }

  private fun getReleaseDateProvider(serviceName: String): FindReleaseDateServiceProvider {
    return if (serviceName == "HISTORIC") {
      historicReleaseDateService
    } else {
      calculateReleaseDateService
    }
  }
}
