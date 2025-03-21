package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiImprisonmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonersearchapi.service.PrisonerSearchApiClient

@Service
class PrisonService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
  fun getCourtDateResults(prisonerId: String): List<PrisonApiCharge> {
    return prisonApiClient.getCourtDateResults(prisonerId)
  }

  fun getOffenderDetail(prisonerId: String): Prisoner {
    return prisonerSearchApiClient.findByPrisonerNumber(prisonerId)
  }

  fun getPrison(agencyId: String): Prison {
    return prisonApiClient.getPrison(agencyId)
  }

  fun getSentencesAndOffences(bookingId: Long, filterActive: Boolean = true): List<SentenceAndOffences> {
    return prisonApiClient.getSentencesAndOffences(bookingId)
      .filter { !filterActive || it.sentenceStatus == "A" }
  }

  fun getImprisonmentStatusHistory(prisonerId: String): List<PrisonApiImprisonmentStatus> {
    return prisonApiClient.getImprisonmentStatusHistory(prisonerId)
  }
}
