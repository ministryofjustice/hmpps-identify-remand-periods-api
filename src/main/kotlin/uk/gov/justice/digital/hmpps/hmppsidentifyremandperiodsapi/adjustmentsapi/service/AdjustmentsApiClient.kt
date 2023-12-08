package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import java.util.UUID

@Service
class AdjustmentsApiClient(@Qualifier("adjustmentsApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getAdjustments(person: String): List<AdjustmentDto> {
    log.info("Requesting adjustments for $person")
    return webClient.get()
      .uri("/adjustments?person=$person")
      .retrieve()
      .bodyToMono(typeReference<List<AdjustmentDto>>())
      .block()!!
  }

  fun createAdjustment(adjustments: List<AdjustmentDto>) {
    log.info("Creating adjustments")
    webClient.post()
      .uri("/adjustments")
      .bodyValue(adjustments)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun delete(id: UUID) {
    log.info("Deleting adjustment")
    webClient.delete()
      .uri("/adjustments/$id")
      .retrieve()
      .toBodilessEntity()
      .block()
  }
}
