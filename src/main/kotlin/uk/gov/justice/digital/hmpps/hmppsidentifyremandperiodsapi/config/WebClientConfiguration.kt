package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${prison.api.url}") private val prisonApiUri: String,
  @Value("\${calculate-release-dates.api.url}") private val calculateReleaseDatesApiUrl: String,
) {

  @Bean
  fun prisonApiWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(prisonApiUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun calculateReleaseDatesApiWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(calculateReleaseDatesApiUrl)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
        .build()
      next.exchange(filtered)
    }
  }
}
