package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonersearchapi.service

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonersearchapi.model.Prisoner

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Service
class PrisonerSearchApiClient(private val prisonerSearchApiWebClient: WebClient) {

  fun findByPrisonerNumber(prisonerNumber: String): Prisoner = prisonerSearchApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/prisoner/{prisonerNumber}")
        .build(prisonerNumber)
    }
    .retrieve()
    .bodyToMono(typeReference<Prisoner>())
    .block()!!
}
