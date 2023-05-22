package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonerDetails

@Service
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getCourtDateResults(prisonerId: String): List<PrisonApiCourtDateResult> {
    log.info("Requesting court case results for prisoner $prisonerId")
    return webClient.get()
      .uri("/api/digital-warrant/court-date-results/$prisonerId")
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiCourtDateResult>>())
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
}
