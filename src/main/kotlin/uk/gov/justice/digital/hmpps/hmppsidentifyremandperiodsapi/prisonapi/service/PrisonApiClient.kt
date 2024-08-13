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
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonerDetails
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

  fun getOffenderDetail(prisonerId: String): PrisonerDetails {
    log.info("Requesting details for prisoner $prisonerId")
    return webClient.get()
      .uri("/api/offenders/$prisonerId")
      .retrieve()
      .bodyToMono(typeReference<PrisonerDetails>())
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
    log.info("Requesting sentence terms for bookingId $bookingId")
    return webClient.get()
      .uri("/api/offender-sentences/booking/$bookingId/sentences-and-offences")
      .retrieve()
      .bodyToMono(typeReference<List<SentenceAndOffences>>())
      .block()!!
  }
}
