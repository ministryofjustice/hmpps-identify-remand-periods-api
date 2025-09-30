package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import com.fasterxml.jackson.core.type.TypeReference
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.TestUtil
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.SentencePeriod

class TestCaseAnonymiser {

  @Test
  fun anonymise() {
    val exampleName = "adjst-1381-wrong-booking-id"
    log.info("Anonymising example $exampleName")

    val example = TestUtil.objectMapper()
      .readValue(ClassPathResource("/data/RemandCalculation/$exampleName.json").file, TestExample::class.java)

    val bookings = mutableMapOf<Long, Booking>()
    val write = example.copy(
      remandCalculation = example.remandCalculation.copy(
        prisonerId = "ABC123",
        chargesAndEvents = example.remandCalculation.chargesAndEvents.map {
          val booking = getBooking(bookings, it.charge.bookingId)
          it.copy(
            charge = it.charge.copy(
              courtCaseRef = null,
              courtLocation = null,
              bookingId = booking.bookingId,
              bookNumber = booking.bookNumber,
            ),
          )
        },
        issuesWithLegacyData = emptyList(),
      ),
      calculations = transformIntersecting(),
    )

    log.info("Anon: ${TestUtil.objectMapper().writeValueAsString(write)}")
    TestUtil.objectMapper()
      .writeValue(ClassPathResource("/data/RemandCalculation/$exampleName.json").file, write)
  }

  private fun getBooking(bookings: MutableMap<Long, Booking>, bookingId: Long): Booking {
    if (!bookings.containsKey(bookingId)) {
      val newId = (bookings.values.maxOfOrNull { it.bookingId } ?: 0) + 1
      bookings[bookingId] = Booking(newId, newId.toString())
    }
    return bookings[bookingId]!!
  }

  private fun transformIntersecting(): List<Calculations> {
    val intersectingJson = """[]""".trimIndent()

    val intersecting = TestUtil.objectMapper()
      .readValue(intersectingJson, object : TypeReference<List<SentencePeriod>>() {})

    return intersecting.map {
      Calculations(
        calculateAt = it.from,
        release = it.to,
        service = it.service,
      )
    }
  }

  private data class Booking(
    val bookingId: Long,
    val bookNumber: String,
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
