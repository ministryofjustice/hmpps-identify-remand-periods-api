package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model.RelevantRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model.RelevantRemandCalculationResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model.RelevantRemandReleaseDateCalculationRequest
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.time.LocalDate

@Service
class CalculateReleaseDateService(
  private val calculateReleaseDatesApiClient: CalculateReleaseDatesApiClient,
) {

  fun calculateReleaseDate(prisonerId: String, remand: List<Remand>, sentence: Sentence, calculateAt: LocalDate, charges: Map<Long, Charge>): LocalDate {
    val request = RelevantRemandReleaseDateCalculationRequest(
      remand.filter { charges[it.chargeId]!!.bookingId == sentence.bookingId }.map { RelevantRemand(it.from, it.to, it.days.toInt(), charges[it.chargeId]!!.sentenceSequence!!) },
      sentence,
      calculateAt,
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
    return if (sentence.recallDate == calculateAt) {
      if (result.postRecallReleaseDate == null) {
        throw UnsupportedCalculationException("CRDS Calculation expected a recall release date, but was not found. $request")
      }
      result.postRecallReleaseDate
    } else {
      if (result.releaseDate == null) {
        throw UnsupportedCalculationException("CRDS Calculation expected a release date, but was not found. $request")
      }
      result.releaseDate
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
