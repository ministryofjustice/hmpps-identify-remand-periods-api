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
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationDetail
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence

class RemandCalculationServiceTest {
  private val calculateReleaseDateService = mock<CalculateReleaseDateService>()
  private val findHistoricReleaseDateService = mock<FindHistoricReleaseDateService>()
  private val findReleaseDateService = FindReleaseDateService(findHistoricReleaseDateService, calculateReleaseDateService)
  private val remandClockService = RemandClockService()
  private val sentenceRemandService = SentenceRemandService(findReleaseDateService)
  private val remandAdjustmentService = RemandAdjustmentService()
  private val chargeRemandStatusService = ChargeRemandStatusService()
  private val resultSortingService = ResultSortingService()
  private val mergeChargeRemandService = MergeChargeRemandService()
  private val chargeCombinationService = ChargeCombinationService()
  private val remandCalculationService = RemandCalculationService(chargeCombinationService, remandClockService, sentenceRemandService, remandAdjustmentService, chargeRemandStatusService, resultSortingService, mergeChargeRemandService)

  @ParameterizedTest
  @CsvFileSource(resources = ["/data/tests.csv"], numLinesToSkip = 1)
  fun `Test Examples`(exampleName: String, error: String?) {
    log.info("Testing example $exampleName")

    val example = TestUtil.objectMapper().readValue(ClassPathResource("/data/RemandCalculation/$exampleName.json").file, TestExample::class.java)

    stubCalculations(exampleName, example)

    val remandResult: RemandResult
    try {
      remandResult = remandCalculationService.calculate(example.remandCalculation, RemandCalculationRequestOptions())
    } catch (e: Exception) {
      if (!error.isNullOrEmpty()) {
        Assertions.assertEquals(error, e.javaClass.simpleName)
        return
      } else {
        throw e
      }
    }

    val expected = TestUtil.objectMapper().readValue(ClassPathResource("/data/RemandResult/$exampleName.json").file, RemandResult::class.java)
    assertThat(remandResult)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes("charges", "remandCalculation")
      .isEqualTo(expected)
  }

  private fun stubCalculations(exampleName: String, example: TestExample) {
    stubErrorCalculationsAsDefault()
    example.sentences.forEach { sentence ->
      sentence.calculations.forEach { calculation ->
        log.info("Stubbing release dates for $exampleName: $sentence $calculation")
        if (calculation.service == "HISTORIC") {
          whenever(
            findHistoricReleaseDateService.findReleaseDate(
              eq(example.remandCalculation.prisonerId),
              any(),
              eq(Sentence(sentence.sentenceSequence, sentence.sentenceAt, sentence.recallDates, sentence.bookingId)),
              eq(calculation.calculateAt),
              any(),
            ),
          ).thenAnswer {
            CalculationDetail(calculation.release)
          }
        } else {
          whenever(
            calculateReleaseDateService.findReleaseDate(
              eq(example.remandCalculation.prisonerId),
              any(),
              eq(Sentence(sentence.sentenceSequence, sentence.sentenceAt, sentence.recallDates, sentence.bookingId)),
              eq(calculation.calculateAt),
              any(),
            ),
          ).thenAnswer {
            CalculationDetail(calculation.release)
          }
        }
      }
    }
  }

  private fun stubErrorCalculationsAsDefault() {
    whenever(
      findHistoricReleaseDateService.findReleaseDate(
        any(),
        any(),
        any(),
        any(),
        any(),
      ),
    ).thenAnswer {
      throw UnsupportedCalculationException("Historic calculation error!")
    }
    whenever(
      calculateReleaseDateService.findReleaseDate(
        any(),
        any(),
        any(),
        any(),
        any(),
      ),
    ).thenAnswer {
      throw UnsupportedCalculationException("CRDS calculation error!")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
