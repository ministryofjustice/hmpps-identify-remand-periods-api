package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.entity.IdentifyRemandDecision
import java.util.UUID

@Repository
interface IdentifyRemandDecisionRepository: JpaRepository<IdentifyRemandDecision, UUID> {

  fun findFirstByPersonOrderByDecisionAtDesc(person: String): IdentifyRemandDecision?
}