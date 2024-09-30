package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform.transform
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.IdentifyRemandDecisionDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service.IdentifyRemandDecisionService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service.RemandCalculationService

@RestController
@RequestMapping("/relevant-remand", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "relevant-remand-controller", description = "Operations involving a calculating relevant remand")
class RelevantRemandController(
  private val remandCalculationService: RemandCalculationService,
  private val prisonService: PrisonService,
  private val remandDecisionService: IdentifyRemandDecisionService,
) {
  @PostMapping(value = ["/{prisonerId}"])
  @PreAuthorize("hasAnyRole('IDENTIFY_REMAND__IDENTIFY_RW', 'IDENTIFY_REMAND__IDENTIFY_RO')")
  @ResponseBody
  @Operation(
    summary = "Calculates relevant remand",
    description = "This endpoint will calculate relevant remand based on the data from NOMIS before returning it to" +
      " the user",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns calculated relevant remand"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun calculate(
    @Parameter(required = true, example = "A1234AB", description = "The prisoners ID (aka nomsId)")
    @PathVariable("prisonerId")
    prisonerId: String,
    @RequestBody
    remandCalculationRequestOptions: RemandCalculationRequestOptions = RemandCalculationRequestOptions(),
  ): RemandResult {
    log.info("Request received to calculate relevant remand for $prisonerId")
    val courtDateResults = prisonService.getCourtDateResults(prisonerId)
    val prisonerDetails = prisonService.getOffenderDetail(prisonerId)
    val sentencesAndOffences = prisonService.getSentencesAndOffences(prisonerDetails.bookingId.toLong(), true)
    return remandCalculationService.calculate(transform(courtDateResults, prisonerDetails, sentencesAndOffences), remandCalculationRequestOptions)
  }

  @PostMapping(value = ["/{prisonerId}/decision"])
  @PreAuthorize("hasAnyRole('IDENTIFY_REMAND__IDENTIFY_RW')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Saves a decision to accept or reject relevant remand",
    description = "This endpoint will save a decision to accept or reject relevant remand, and also call the adjustments api to save the data.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Decision created okay."),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun saveDecision(
    @Parameter(required = true, example = "A1234AB", description = "The prisoners ID (aka nomsId)")
    @PathVariable("prisonerId")
    prisonerId: String,
    @RequestBody remandDecision: IdentifyRemandDecisionDto,
  ): IdentifyRemandDecisionDto {
    return remandDecisionService.saveDecision(prisonerId, remandDecision)
  }

  @GetMapping(value = ["/{prisonerId}/decision"])
  @PreAuthorize("hasAnyRole('IDENTIFY_REMAND__IDENTIFY_RW', 'IDENTIFY_REMAND__IDENTIFY_RO')")
  @Operation(
    summary = "Get the latest decision for a given person",
    description = "This endpoint return the latest decision for a given person.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Gets latest remand decision for person."),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getDecision(
    @Parameter(required = true, example = "A1234AB", description = "The prisoners ID (aka nomsId)")
    @PathVariable("prisonerId")
    prisonerId: String,
  ): IdentifyRemandDecisionDto? {
    return remandDecisionService.getDecision(prisonerId)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
