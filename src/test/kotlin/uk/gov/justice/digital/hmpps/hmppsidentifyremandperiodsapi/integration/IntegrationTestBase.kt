package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock.AdjustmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock.CalculateReleaseDatesApiExtension
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock.PrisonerSearchApiExtension

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthApiExtension::class, PrisonApiExtension::class, CalculateReleaseDatesApiExtension::class, AdjustmentsApiExtension::class, PrisonerSearchApiExtension::class)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAuthorisationRemandToolUser(
    user: String = "test-client",
    roles: List<String> = listOf("ROLE_IDENTIFY_REMAND__IDENTIFY_RW"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
}
