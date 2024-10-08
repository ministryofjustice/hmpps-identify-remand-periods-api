package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LocalDateExtTest {

  @Test
  fun `isBeforeOrEqualTo test`() {
    val testDate = LocalDate.of(2021, 1, 1)
    val pastDate = LocalDate.of(2020, 1, 1)
    val equalDate = LocalDate.of(2021, 1, 1)
    val futureDate = LocalDate.of(2022, 1, 1)

    assertFalse(testDate.isBeforeOrEqualTo(pastDate))
    assertTrue(testDate.isBeforeOrEqualTo(equalDate))
    assertTrue(testDate.isBeforeOrEqualTo(futureDate))
  }

  @Test
  fun `isAfterOrEqualTo test`() {
    val testDate = LocalDate.of(2021, 1, 1)
    val pastDate = LocalDate.of(2020, 1, 1)
    val equalDate = LocalDate.of(2021, 1, 1)
    val futureDate = LocalDate.of(2022, 1, 1)

    assertTrue(testDate.isAfterOrEqualTo(pastDate))
    assertTrue(testDate.isAfterOrEqualTo(equalDate))
    assertFalse(testDate.isAfterOrEqualTo(futureDate))
  }
}
