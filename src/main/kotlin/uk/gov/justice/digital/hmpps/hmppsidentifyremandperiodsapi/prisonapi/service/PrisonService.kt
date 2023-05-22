package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonerDetails

@Service
class PrisonService(
  private val prisonApiClient: PrisonApiClient,
) {
  fun getCourtDateResults(prisonerId: String): List<PrisonApiCourtDateResult> {
    return prisonApiClient.getCourtDateResults(prisonerId)
  }

  fun getOffenderDetail(prisonerId: String): PrisonerDetails {
    return prisonApiClient.getOffenderDetail(prisonerId)
  }
}
