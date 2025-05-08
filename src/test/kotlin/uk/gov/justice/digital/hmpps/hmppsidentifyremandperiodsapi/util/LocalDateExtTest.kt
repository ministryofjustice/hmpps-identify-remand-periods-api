package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util

import org.assertj.core.api.Assertions
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

  @Test
  fun `mojDisplayFormat test`() {
    val one = LocalDate.of(2021, 1, 1)
    val two = LocalDate.of(2020, 1, 1)
    val three = LocalDate.of(2022, 1, 1)

    Assertions.assertThat(one.mojDisplayFormat()).isEqualTo("1 Jan 2021")
    Assertions.assertThat(two.mojDisplayFormat()).isEqualTo("1 Jan 2020")
    Assertions.assertThat(three.mojDisplayFormat()).isEqualTo("1 Jan 2022")
  }
}
