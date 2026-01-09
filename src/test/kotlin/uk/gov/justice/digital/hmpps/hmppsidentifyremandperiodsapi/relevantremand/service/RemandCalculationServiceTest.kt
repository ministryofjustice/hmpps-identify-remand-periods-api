package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.util.stream.Stream

class RemandCalculationServiceTest {
  private val calculateReleaseDateService = mock<CalculateReleaseDateService>()
  private val findHistoricReleaseDateService = mock<FindHistoricReleaseDateService>()
  private val findReleaseDateService =
    FindReleaseDateService(findHistoricReleaseDateService, calculateReleaseDateService)
  private val remandClockService = RemandClockService()
  private val sentenceRemandService = SentenceRemandService(findReleaseDateService)
  private val remandAdjustmentService = RemandAdjustmentService()
  private val chargeRemandStatusService = ChargeRemandStatusService()
  private val resultSortingService = ResultSortingService()
  private val mergeChargeRemandService = MergeChargeRemandService()
  private val relatedChargeCombinationService = RelatedChargeCombinationService()
  private val userSelectedCombinationService = UserSelectedCombinationService()
  private val validationChargeService = ValidateCalculationDataService()
  private val remandCalculationService = RemandCalculationService(
    relatedChargeCombinationService,
    userSelectedCombinationService,
    remandClockService,
    sentenceRemandService,
    remandAdjustmentService,
    chargeRemandStatusService,
    resultSortingService,
    mergeChargeRemandService,
    validationChargeService,
  )

  @ParameterizedTest
  @MethodSource(value = ["testCaseSource"])
  fun `Test Examples`(exampleName: String) {
    log.info("Testing example $exampleName")

    val example = TestUtil.objectMapper()
      .readValue(ClassPathResource("/data/RemandCalculation/$exampleName").file, TestExample::class.java)

    stubCalculations(exampleName, example)

    val remandResult: RemandResult
    try {
      remandResult = remandCalculationService.calculate(example.remandCalculation, example.options)
    } catch (e: Exception) {
      if (!example.error.isNullOrEmpty()) {
        assertThat(e.javaClass.simpleName).contains(example.error.split("(").first())
        assertThat(e.message).contains(example.error.split("(")[1])
        return
      } else {
        throw e
      }
    }

    log.info("Actual result:\n ${TestUtil.objectMapper().writeValueAsString(remandResult.copy(charges = emptyMap()))}")

    val expected = TestUtil.objectMapper()
      .readValue(ClassPathResource("/data/RemandResult/$exampleName").file, RemandResult::class.java)
    assertThat(remandResult)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes("charges", "remandCalculation")
      .isEqualTo(expected)
  }

  private fun stubCalculations(exampleName: String, example: TestExample) {
    stubErrorCalculationsAsDefault()
    example.calculations.forEach { calculation ->
      log.info("Stubbing release dates for $exampleName: $calculation")
      if (calculation.unusedDeductions != null) {
        stubCrds(example, calculation)
      }
      if (calculation.service == "HISTORIC") {
        stubHistoric(example, calculation)
      } else {
        stubCrds(example, calculation)
      }
    }
  }
  private fun stubCrds(example: TestExample, calculation: Calculations) {
    whenever(
      calculateReleaseDateService.findReleaseDate(
        eq(example.remandCalculation.prisonerId),
        any(),
        any<List<Sentence>>(),
        eq(calculation.calculateAt),
        any(),
      ),
    ).thenAnswer {
      CalculationDetail(calculation.release, unusedDeductions = calculation.unusedDeductions)
    }
  }

  private fun stubHistoric(example: TestExample, calculation: Calculations) {
    whenever(
      findHistoricReleaseDateService.findReleaseDate(
        eq(example.remandCalculation.prisonerId),
        any(),
        any(),
        eq(calculation.calculateAt),
        any(),
      ),
    ).thenAnswer {
      CalculationDetail(calculation.release)
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
        any<List<Sentence>>(),
        any(),
        any(),
      ),
    ).thenAnswer {
      throw UnsupportedCalculationException("CRDS calculation error!")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    @JvmStatic
    fun testCaseSource(): Stream<Arguments> {
      val args = mutableListOf<String>()
      TestUtil.doAllInDir(
        "/data/RemandCalculation",
      ) {
        args.add(it.name)
      }
      return args.stream().map { Arguments.of(it, null) }
    }
  }
}
