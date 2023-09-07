package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto
import java.time.LocalDate
import java.util.UUID

class AdjustmentsServiceTest {
  private val apiClient = mock<AdjustmentsApiClient>()
  private val adjustmentsService = AdjustmentsService(apiClient)

  private val person = "ABC123"

  @Test
  fun `Service will create, delete and leave existing matching remand alone`() {
    val adjustmentExistingMatches = AdjustmentDto(
      id = UUID.randomUUID(),
      fromDate = LocalDate.now().minusDays(10),
      toDate = LocalDate.now().minusDays(9),
      bookingId = 1,
      sentenceSequence = 1,
      person = person,
      prisonId = "BMI",
    )
    val adjustmentExistingNotMatching = AdjustmentDto(
      id = UUID.randomUUID(),
      fromDate = LocalDate.now().minusDays(8),
      toDate = LocalDate.now().minusDays(7),
      bookingId = 1,
      sentenceSequence = 1,
      person = person,
      prisonId = "BMI",
    )
    val newAdjustment = AdjustmentDto(
      id = null,
      fromDate = LocalDate.now().minusDays(6),
      toDate = LocalDate.now().minusDays(5),
      bookingId = 1,
      sentenceSequence = 1,
      person = person,
      prisonId = "BMI",
    )

    whenever(apiClient.getAdjustments(person)).thenReturn(listOf(adjustmentExistingMatches, adjustmentExistingNotMatching, adjustmentExistingMatches.copy(adjustmentType = "NOT_REMAND")))

    adjustmentsService.saveRemand(person, listOf(adjustmentExistingMatches.copy(id = null), newAdjustment))

    verify(apiClient, times(1)).delete(adjustmentExistingNotMatching.id!!)
    verify(apiClient, times(1)).createAdjustment(newAdjustment)
  }
}
