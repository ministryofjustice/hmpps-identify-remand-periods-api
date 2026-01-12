package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model.RelevantRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model.RelevantRemandCalculationResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model.RelevantRemandReleaseDateCalculationRequest
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationDetail
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service.FindReleaseDateServiceProvider
import java.time.LocalDate

@Service
class CalculateReleaseDateService(
  private val calculateReleaseDatesApiClient: CalculateReleaseDatesApiClient,
) : FindReleaseDateServiceProvider {

  override fun findReleaseDate(prisonerId: String, remand: List<Remand>, sentences: List<Sentence>, calculatedAt: LocalDate, charges: Map<Long, Charge>): CalculationDetail {
    val releaseDates = sentences.map { findReleaseDate(prisonerId, remand, it, calculatedAt, charges) }
    return CalculationDetail(releaseDates.minOf { it.releaseDate }, unusedDeductions = releaseDates.mapNotNull { it.unusedDeductions }.maxOrNull())
  }

  private fun findReleaseDate(prisonerId: String, remand: List<Remand>, sentence: Sentence, calculatedAt: LocalDate, charges: Map<Long, Charge>): ReleaseAndUnusedDeductions {
    if (sentence.recallDates.size > 1 && sentence.recallDates.dropLast(1).any { it == calculatedAt }) {
      throw UnsupportedCalculationException("CRDS cannot calculate can only calculate the most recent recall date. Not previous FTRs")
    }

    val request = RelevantRemandReleaseDateCalculationRequest(
      remand.filter { charges[it.chargeId]!!.bookingId == sentence.bookingId }.map { RelevantRemand(it.from, it.to, it.days.toInt(), charges[it.chargeId]!!.sentenceSequence!!) },
      sentence,
      calculatedAt,
    )
    val result: RelevantRemandCalculationResult
    try {
      result = calculateReleaseDatesApiClient.calculateReleaseDates(prisonerId, request)
    } catch (e: Exception) {
      throw UnsupportedCalculationException("Error calling CRDS $request", e)
    }
    if (result.validationMessages.isNotEmpty()) {
      throw UnsupportedCalculationException(
        "Validation error from calling CRDS $sentence, \n ${
          result.validationMessages.joinToString(
            separator = "\n",
          ) { it.message }
        }",
      )
    }
    return if (sentence.recallDates.lastOrNull() == calculatedAt) {
      if (result.postRecallReleaseDate == null) {
        throw UnsupportedCalculationException("CRDS Calculation expected a recall release date, but was not found. $request")
      }
      ReleaseAndUnusedDeductions(result.postRecallReleaseDate, result.unusedDeductions)
    } else {
      if (result.releaseDate == null) {
        throw UnsupportedCalculationException("CRDS Calculation expected a release date, but was not found. $request")
      }
      ReleaseAndUnusedDeductions(result.releaseDate, result.unusedDeductions)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private data class ReleaseAndUnusedDeductions(
  val releaseDate: LocalDate,
  val unusedDeductions: Long?,
)
