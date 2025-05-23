package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.OffenderKeyDates
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiExternalMovement
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiImprisonmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.SentenceCalculationSummary

@Service
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getCourtDateResults(prisonerId: String): List<PrisonApiCharge> {
    log.info("Requesting court case results for prisoner $prisonerId")
    return webClient.get()
      .uri("/api/court-date-results/by-charge/$prisonerId")
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiCharge>>())
      .block()!!
  }

  fun getPrison(prisonId: String): Prison {
    log.info("Requesting details for prisonId $prisonId")
    return webClient.get()
      .uri("/api/agencies/$prisonId?activeOnly=false")
      .retrieve()
      .bodyToMono(typeReference<Prison>())
      .block()!!
  }

  fun getCalculationsForAPrisonerId(prisonerId: String): List<SentenceCalculationSummary> {
    log.info("Requesting calculations for prisoner $prisonerId")
    return webClient.get()
      .uri { uriBuilder ->
        uriBuilder.path("/api/offender-dates/calculations/$prisonerId")
          .queryParam("latestOnly", "false")
          .build()
      }
      .retrieve()
      .bodyToMono(typeReference<List<SentenceCalculationSummary>>())
      .block()!!
  }

  fun getNOMISOffenderKeyDates(offenderSentCalcId: Long): OffenderKeyDates {
    log.info("Requesting calculation key dates for $offenderSentCalcId")
    return webClient.get()
      .uri { uriBuilder ->
        uriBuilder.path("/api/offender-dates/sentence-calculation/$offenderSentCalcId")
          .build()
      }
      .retrieve()
      .bodyToMono(typeReference<OffenderKeyDates>())
      .block()!!
  }

  fun getSentencesAndOffences(bookingId: Long): List<SentenceAndOffences> {
    log.info("Requesting sentences and offences for bookingId $bookingId")
    return webClient.get()
      .uri("/api/offender-sentences/booking/$bookingId/sentences-and-offences")
      .retrieve()
      .bodyToMono(typeReference<List<SentenceAndOffences>>())
      .block()!!
  }

  fun getImprisonmentStatusHistory(prisonerId: String): List<PrisonApiImprisonmentStatus> {
    log.info("Requesting imprisonment status history for $prisonerId")
    return webClient.get()
      .uri("/api/imprisonment-status-history/$prisonerId")
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiImprisonmentStatus>>())
      .block()!!
  }

  fun getExternalMovements(prisonerId: String): List<PrisonApiExternalMovement> {
    log.info("Requesting external movements for $prisonerId")
    return webClient.get()
      .uri("/api/movements/offender/$prisonerId?allBookings=true&movementTypes=ADM&movementTypes=REL")
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiExternalMovement>>())
      .block()!!
  }
}
