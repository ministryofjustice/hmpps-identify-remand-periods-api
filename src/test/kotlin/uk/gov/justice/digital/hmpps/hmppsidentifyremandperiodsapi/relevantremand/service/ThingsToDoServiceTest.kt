package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.RemandDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.UnusedDeductionsCalculationResultDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.service.AdjustmentsService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemandStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.IdentifyRemandDecisionDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandApplicableUserSelection
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ThingsToDo
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ToDoType
import java.time.LocalDate

class ThingsToDoServiceTest {

  private val identifyRemandDecisionService: IdentifyRemandDecisionService = mock()
  private val remandCalculationService: RemandCalculationService = mock()
  private val adjustmentService: AdjustmentsService = mock()

  private val service = ThingsToDoService(identifyRemandDecisionService, remandCalculationService, adjustmentService)

  private val remandCalculation = RemandCalculation(
    prisonerId = PRISONER_ID,
    imprisonmentStatuses = emptyList(),
    issuesWithLegacyData = emptyList(),
    chargesAndEvents = emptyList(),
  )

  @Nested
  inner class NoDecisionTests {
    @Test
    fun `getToDoList first time review with days`() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(null)
      whenever(remandCalculationService.calculate(remandCalculation, RemandCalculationRequestOptions())).thenReturn(
        remandResultWithMatchingDays,
      )
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )

      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_FIRST_TIME), 32))
    }

    @Test
    fun `getToDoList first time review with zero days but could have upgrade downgrade scenario`() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(null)
      whenever(remandCalculationService.calculate(remandCalculation, RemandCalculationRequestOptions())).thenReturn(
        remandResultNoDaysUpgradeDowngrade,
      )
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )

      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(
        ThingsToDo(
          PRISONER_ID,
          listOf(ToDoType.IDENTIFY_REMAND_REVIEW_FIRST_TIME_UPGRADE_DOWNGRADE),
          0,
        ),
      )
    }

    @Test
    fun `getToDoList first time review with zero days or upgrade downgrade scenario`() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(null)
      whenever(remandCalculationService.calculate(remandCalculation, RemandCalculationRequestOptions())).thenReturn(
        remandResultEmpty,
      )
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )

      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID))
    }
  }

  @Nested
  inner class DecisionTests {
    @Test
    fun `getToDoList rejected decision matching days `() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(rejectedDecision)
      whenever(remandCalculationService.calculate(remandCalculation, RemandCalculationRequestOptions())).thenReturn(
        remandResultWithMatchingDays,
      )
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )
      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID))
    }

    @Test
    fun `getToDoList rejected decision non matching days `() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(rejectedDecision)
      whenever(remandCalculationService.calculate(remandCalculation, RemandCalculationRequestOptions())).thenReturn(
        remandResultWithNonMatchingDays,
      )
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )
      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_UPDATE), 153))
    }

    @Test
    fun `getToDoList accepted decision matching days but not matching adjustments`() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(acceptedDecision)
      whenever(remandCalculationService.calculate(remandCalculation, nonDefaultOptions)).thenReturn(
        remandResultWithMatchingDays,
      )
      whenever(adjustmentService.getRemandAdjustments(PRISONER_ID)).thenReturn(nonMatchingAdjustments)
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )
      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_UPDATE), 32))
    }

    @Test
    fun `getToDoList accepted decision matching days and matching adjustments`() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(acceptedDecision)
      whenever(remandCalculationService.calculate(remandCalculation, nonDefaultOptions)).thenReturn(
        remandResultWithMatchingDays,
      )
      whenever(adjustmentService.getRemandAdjustments(PRISONER_ID)).thenReturn(matchingAdjustments)
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )
      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID))
    }

    @Test
    fun `getToDoList accepted decision non matching days`() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(acceptedDecision)
      whenever(remandCalculationService.calculate(remandCalculation, nonDefaultOptions)).thenReturn(
        remandResultWithNonMatchingDays,
      )
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsCalculatedStatus,
      )
      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID, listOf(ToDoType.IDENTIFY_REMAND_REVIEW_UPDATE), 153))
    }

    @Test
    fun `getToDoList if a validation result is returned then there are no thingsToDo`() {
      whenever(identifyRemandDecisionService.getDecision(PRISONER_ID)).thenReturn(acceptedDecision)
      whenever(remandCalculationService.calculate(remandCalculation, nonDefaultOptions)).thenReturn(
        remandResultWithNonMatchingDays,
      )
      whenever(adjustmentService.getUnusedDeductionsCalculationResult(PRISONER_ID)).thenReturn(
        unusedDeductionsValidationStatus,
      )
      val result = service.getToDoList(remandCalculation)

      assertThat(result).isEqualTo(ThingsToDo(PRISONER_ID))
    }
  }

  companion object {
    private const val PRISONER_ID = "ABC123"
    private val nonMatchingAdjustments = listOf(
      AdjustmentDto(
        id = null,
        bookingId = 1L,
        sentenceSequence = 1,
        person = PRISONER_ID,
        fromDate = LocalDate.of(2024, 1, 1),
        toDate = LocalDate.of(2024, 6, 1),
        remand = RemandDto(
          listOf(1L),
        ),
        status = AdjustmentStatus.ACTIVE,
      ),
    )
    private val matchingAdjustments = listOf(
      AdjustmentDto(
        id = null,
        bookingId = 1L,
        sentenceSequence = 1,
        person = PRISONER_ID,
        fromDate = LocalDate.of(2024, 1, 1),
        toDate = LocalDate.of(2024, 2, 1),
        remand = RemandDto(
          listOf(1L),
        ),
        status = AdjustmentStatus.ACTIVE,
      ),
    )
    private val remandResultWithMatchingDays = RemandResult(
      issuesWithLegacyData = emptyList(),
      charges = emptyMap(),
      intersectingSentences = emptyList(),
      chargeRemand = emptyList(),
      adjustments = matchingAdjustments,
    )
    private val remandResultWithNonMatchingDays = RemandResult(
      issuesWithLegacyData = emptyList(),
      charges = emptyMap(),
      intersectingSentences = emptyList(),
      chargeRemand = emptyList(),
      adjustments = nonMatchingAdjustments,
    )
    private val remandResultNoDaysUpgradeDowngrade = RemandResult(
      issuesWithLegacyData = emptyList(),
      charges = emptyMap(),
      intersectingSentences = emptyList(),
      chargeRemand = listOf(
        ChargeRemand(
          from = LocalDate.of(2024, 1, 1),
          to = LocalDate.of(2024, 2, 1),
          status = ChargeRemandStatus.NOT_SENTENCED,
          chargeIds = listOf(1L),
          fromEvent = CourtAppearance(LocalDate.of(2024, 1, 1), "ASD"),
          toEvent = CourtAppearance(LocalDate.of(2024, 2, 1), "ASD"),
        ),
      ),
      adjustments = emptyList(),
    )
    private val remandResultEmpty = RemandResult(
      issuesWithLegacyData = emptyList(),
      charges = emptyMap(),
      intersectingSentences = emptyList(),
      chargeRemand = emptyList(),
      adjustments = emptyList(),
    )
    private val nonDefaultOptions = RemandCalculationRequestOptions(
      userSelections = listOf(RemandApplicableUserSelection(listOf(1L, 2L, 3L), 4L)),
    )
    private val rejectedDecision = IdentifyRemandDecisionDto(
      options = nonDefaultOptions,
      accepted = false,
      rejectComment = "Not right",
      days = 32,
    )
    private val acceptedDecision = IdentifyRemandDecisionDto(
      options = nonDefaultOptions,
      accepted = true,
      rejectComment = null,
      days = 32,
    )
    private val unusedDeductionsCalculatedStatus = UnusedDeductionsCalculationResultDto(
      status = "CALCULATED",
      person = PRISONER_ID,
    )
    private val unusedDeductionsValidationStatus = UnusedDeductionsCalculationResultDto(
      status = "VALIDATION",
      person = PRISONER_ID,
    )
  }
}
