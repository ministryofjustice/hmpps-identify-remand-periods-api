package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform.transform
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service.RemandCalculationService

@RestController
@RequestMapping("/relevant-remand", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "relevant-remand-controller", description = "Operations involving a calculating relevant remand")
class RelevantRemandController(
  private val remandCalculationService: RemandCalculationService,
  private val prisonService: PrisonService,
) {
  @PostMapping(value = ["/{prisonerId}"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'MANAGE_DIGITAL_WARRANT')")
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
  ): RemandResult {
    log.info("Request received to calculate relevant remand for $prisonerId")
    val courtDateResults = prisonService.getCourtDateResults(prisonerId)
    val prisonerDetails = prisonService.getOffenderDetail(prisonerId)
    return remandCalculationService.calculate(transform(courtDateResults, prisonerDetails))
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
