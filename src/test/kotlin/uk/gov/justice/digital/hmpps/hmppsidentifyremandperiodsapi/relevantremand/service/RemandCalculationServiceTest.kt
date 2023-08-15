package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.TestUtil
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.service.CalculateReleaseDateService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence

class RemandCalculationServiceTest {
  private val calculateReleaseDateService = mock<CalculateReleaseDateService>()
  private val sentenceRemandService = SentenceRemandService(calculateReleaseDateService)
  private val remandCalculationService = RemandCalculationService(sentenceRemandService)

  @ParameterizedTest
  @CsvFileSource(resources = ["/data/tests.csv"], numLinesToSkip = 1)
  fun `Test Examples`(exampleName: String, error: String?) {
    log.info("Testing example $exampleName")

    val example = TestUtil.objectMapper().readValue(ClassPathResource("/data/RemandCalculation/$exampleName.json").file, TestExample::class.java)

    example.sentences.forEach { sentence ->
      sentence.calculations.forEach { calculation ->
        log.info("Stubbing release dates for $exampleName: $sentence $calculation")
        whenever(
          calculateReleaseDateService.calculateReleaseDate(
            eq(example.remandCalculation.prisonerId),
            any(),
            eq(Sentence(sentence.sentenceSequence, sentence.sentenceAt, sentence.recallDate, sentence.bookingId)),
            eq(calculation.calculateAt),
          ),
        ).thenAnswer {
          if (calculation.calculateAt == sentence.sentenceAt) {
            calculation.release
          } else {
            calculation.postRecallReleaseDate
          }
        }
      }
    }

    val remandResult: RemandResult
    try {
      remandResult = remandCalculationService.calculate(example.remandCalculation)
    } catch (e: Exception) {
      if (!error.isNullOrEmpty()) {
        Assertions.assertEquals(error, e.javaClass.simpleName)
        return
      } else {
        throw e
      }
    }
    println(TestUtil.objectMapper().writeValueAsString(remandResult.intersectingSentences))
    val expected = TestUtil.objectMapper().readValue(ClassPathResource("/data/RemandResult/$exampleName.json").file, RemandResult::class.java)
    assertThat(remandResult).isEqualTo(expected)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
