package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult

@Service
class ResultSortingService {

  fun sort(
    result: RemandResult,
  ): RemandResult {
    return result.copy(
      adjustments = result.adjustments.sortedBy { it.fromDate },
      chargeRemand = result.chargeRemand.sortedWith(compareBy({ it.status }, { it.from })),
      intersectingSentences = result.intersectingSentences.sortedBy { it.from },
    )
  }
}
