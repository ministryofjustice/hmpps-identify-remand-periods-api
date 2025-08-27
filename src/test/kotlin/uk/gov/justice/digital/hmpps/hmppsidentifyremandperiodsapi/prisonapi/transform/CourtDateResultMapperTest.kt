package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateOutcome
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeLegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import java.time.LocalDate

class CourtDateResultMapperTest {

  @Test
  fun `should report unknown outcomes as an issue`() {
    val issuesWithLegacyData = mutableListOf<LegacyDataProblem>()
    val charge = PrisonApiCharge(
      chargeId = 1,
      offenceCode = "O1",
      offenceStatue = "",
      offenceDescription = "An offence",
      guilty = false,
      courtCaseId = 1,
      bookingId = 1,
      bookNumber = "12346ab",
      outcomes = emptyList(),
      offenceDate = LocalDate.of(2020, 12, 25),
      offenceEndDate = null,
      courtCaseRef = null,
      courtLocation = null,
      sentenceSequence = null,
      sentenceDate = null,
      resultCode = "FOO",
      resultDescription = "Foo Description",
      resultDispositionCode = "BAR",
      sentenceType = null,
    )
    val courtDateResult = PrisonApiCourtDateOutcome(
      1,
      LocalDate.of(2021, 1, 2),
      "FOO",
      "Foo Description",
      "BAR",
    )
    val courtDateType = mapCourtDateResult(courtDateResult, charge, issuesWithLegacyData)
    assertThat(courtDateType).isNull()
    assertThat(issuesWithLegacyData).hasSize(1).containsExactly(
      ChargeLegacyDataProblem(LegacyDataProblemType.UNSUPPORTED_OUTCOME, "There is an unsupported outcome for the court event on 2 Jan 2021 within booking 12346ab. This court event was for the offence ‘An offence’ committed on 25 Dec 2020.", charge),
    )
  }
}
