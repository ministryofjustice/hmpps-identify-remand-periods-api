package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.time.LocalDate

interface FindReleaseDateServiceProvider {
  fun findReleaseDate(prisonerId: String, remand: List<Remand>, sentence: Sentence, calculateAt: LocalDate, charges: Map<Long, Charge>): LocalDate
}