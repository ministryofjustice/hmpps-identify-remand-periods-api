package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentStatus
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.service.AdjustmentsService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.service.PrisonService
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform.transform
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.entity.IdentifyRemandDecision
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.IdentifyRemandDecisionDto
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.repository.IdentifyRemandDecisionRepository

@Service
class IdentifyRemandDecisionService(
  val prisonService: PrisonService,
  val remandCalculationService: RemandCalculationService,
  val identifyRemandDecisionRepository: IdentifyRemandDecisionRepository,
  val adjustmentsService: AdjustmentsService,
) {

  fun saveDecision(person: String, decision: IdentifyRemandDecisionDto): IdentifyRemandDecisionDto {
    val courtDateResults = prisonService.getCourtDateResults(person)
    val prisonerDetails = prisonService.getOffenderDetail(person)
    val sentencesAndOffences = prisonService.getSentencesAndOffences(prisonerDetails.bookingId, true)
    val calculation = remandCalculationService.calculate(transform(courtDateResults, prisonerDetails, sentencesAndOffences))
    val days = calculation.sentenceRemand.map { it.days }.reduceOrNull { acc, it -> acc + it } ?: 0

    if (decision.accepted) {
      adjustmentsService.saveRemand(
        person,
        calculation.adjustments.filter { it.status == AdjustmentStatus.ACTIVE },
      )
    }

    val result = identifyRemandDecisionRepository.save(
      IdentifyRemandDecision(
        accepted = decision.accepted,
        rejectComment = if (!decision.accepted) decision.rejectComment else null,
        person = person,
        days = days.toInt(),
        decisionByUsername = getCurrentAuthentication().principal,
        decisionByPrisonId = prisonerDetails.agencyId,
      ),
    )
    return mapToDto(result)
  }

  fun getDecision(person: String): IdentifyRemandDecisionDto? {
    val decision = identifyRemandDecisionRepository.findFirstByPersonOrderByDecisionAtDesc(person)
    if (decision != null) {
      return mapToDto(decision)
    }
    return null
  }

  private fun mapToDto(decision: IdentifyRemandDecision): IdentifyRemandDecisionDto {
    val prisonDescription = decision.decisionByPrisonId?.let { prisonService.getPrison(decision.decisionByPrisonId!!).description }
    return IdentifyRemandDecisionDto(
      accepted = decision.accepted,
      rejectComment = decision.rejectComment,
      days = decision.days,
      decisionOn = decision.decisionAt,
      decisionBy = decision.decisionByUsername,
      decisionByPrisonId = decision.decisionByPrisonId,
      decisionByPrisonDescription = prisonDescription,
    )
  }

  fun getCurrentAuthentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw IllegalStateException("User is not authenticated")
}
