package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationDetail
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Remand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.time.LocalDate

interface FindReleaseDateServiceProvider {
  fun findReleaseDate(prisonerId: String, remand: List<Remand>, sentences: List<Sentence>, calculatedAt: LocalDate, charges: Map<Long, Charge>): CalculationDetail
}
