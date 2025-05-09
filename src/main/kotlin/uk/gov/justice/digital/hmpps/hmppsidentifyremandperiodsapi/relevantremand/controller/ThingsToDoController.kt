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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform.transform
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ThingsToDo
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service.ThingsToDoService

@RestController
@RequestMapping("/things-to-do", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ThingsToDoController", description = "Operations related to the things-to-do list for prisoners")
class ThingsToDoController(
  private val prisonService: PrisonService,
  private val thingsToDoService: ThingsToDoService,
) {
  @GetMapping("/prisoner/{prisonerId}")
  @PreAuthorize("hasRole('IDENTIFY_REMAND__IDENTIFY_RO')")
  @ResponseBody
  @Operation(
    summary = "Retrieve things-to-do for a prisoner",
    description = "Provides a list of things-to-do for a specified prisoner based on their ID.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Successfully returns the things-to-do list"),
      ApiResponse(responseCode = "401", description = "Unauthorized - valid Oauth2 token required"),
      ApiResponse(responseCode = "403", description = "Forbidden - requires appropriate role"),
    ],
  )
  fun getThingsToDo(
    @Parameter(required = true, example = "A1234AB", description = "Prisoner's ID (also known as nomsId)")
    @PathVariable prisonerId: String,
  ): ThingsToDo {
    log.info("Request to retrieve things-to-do list for prisoner ID: {}", prisonerId)
    val courtDateResults = prisonService.getCourtDateResults(prisonerId)
    val prisonerDetails = prisonService.getOffenderDetail(prisonerId)
    val sentencesAndOffences = prisonService.getSentencesAndOffences(prisonerDetails.bookingId.toLong(), true)
    val imprisonmentStatuses = prisonService.getImprisonmentStatusHistory(prisonerId)
    val externalMovement = prisonService.getExternalMovements(prisonerId)
    return thingsToDoService.getToDoList(transform(courtDateResults, prisonerDetails, sentencesAndOffences, imprisonmentStatuses, externalMovement))
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(ThingsToDoController::class.java)
  }
}
