package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service.CalculateReleaseDateService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceAndCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentencePeriod
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentenceRemandLoopTracker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val sentencesToCalculate = sentences.filter { it.sentence.sentenceDate == date || it.sentence.recallDate == date }.distinctBy { it.sentence.bookingId }
    loopTracker.periodsServingSentence.addAll(
      sentencesToCalculate.mapNotNull {
        this.findReleaseDate(date, it, loopTracker, remandCalculation)
      },
    )
  }

  private fun findReleaseDate(
    date: LocalDate,
    sentence: SentenceAndCharge,
    loopTracker: SentenceRemandLoopTracker,
    remandCalculation: RemandCalculation,
  ): SentencePeriod? {
    try {
      val calculation = getReleaseDateProvider(primaryReleaseDateService).findReleaseDate(remandCalculation.prisonerId, loopTracker.final, sentence.sentence, date, remandCalculation.charges)
      return SentencePeriod(date, calculation.releaseDate, sentence.sentence, sentence.charge.chargeId, primaryReleaseDateService, emptyList(), calculation.calculationIds)
    } catch (e: UnsupportedCalculationException) {
      try {
        val calculation = getReleaseDateProvider(secondaryReleaseDateService).findReleaseDate(
          remandCalculation.prisonerId,
          loopTracker.final,
          sentence.sentence,
          date,
          remandCalculation.charges,
        )
        return SentencePeriod(
          date,
          calculation.releaseDate,
          sentence.sentence,
          sentence.charge.chargeId,
          secondaryReleaseDateService,
          listOf(e.message),
          calculation.calculationIds,
        )
      } catch (e: UnsupportedCalculationException) {
        remandCalculation.issuesWithLegacyData.add(
          LegacyDataProblem(
            LegacyDataProblemType.RELEASE_DATE_CALCULATION,
            "Unable to calculate release date on ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} within booking ${sentence.charge.bookNumber}",
            sentence.charge,
          ),
        )
        return null
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
