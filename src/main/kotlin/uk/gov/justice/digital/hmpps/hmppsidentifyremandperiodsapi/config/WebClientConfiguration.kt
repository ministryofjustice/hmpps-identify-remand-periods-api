package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${prison.api.url}") private val prisonApiUri: String,
  @Value("\${calculate-release-dates.api.url}") private val calculateReleaseDatesApiUrl: String,
  @Value("\${adjustments.api.url}") private val adjustmentsApiClient: String,
) {

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val filter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    filter.setDefaultClientRegistrationId("hmpps-api")
    return WebClient.builder()
      .baseUrl(prisonApiUri)
      .filter(filter)
      .build()
  }

  @Bean
  fun adjustmentsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val filter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    filter.setDefaultClientRegistrationId("hmpps-api")
    return WebClient.builder()
      .baseUrl(adjustmentsApiClient)
      .filter(filter)
      .build()
  }

  @Bean
  fun calculateReleaseDatesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val filter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    filter.setDefaultClientRegistrationId("hmpps-api")
    return WebClient.builder()
      .baseUrl(calculateReleaseDatesApiUrl)
      .filter(filter)
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
    clientRegistrationRepository,
    oAuth2AuthorizedClientService,
  ).apply {
    setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build())
  }
}
