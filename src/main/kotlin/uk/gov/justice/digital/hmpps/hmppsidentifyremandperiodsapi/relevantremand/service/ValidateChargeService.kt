package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation

@Service
class ValidateChargeService {

  fun validate(remandCalculation: RemandCalculation) {
    val recalledCharges = remandCalculation.chargesAndEvents.filter { it.charge.isRecallSentence }
    val missingRecallEvents = recalledCharges.filter { it.dates.none { event -> event.isRecallEvent } }

    missingRecallEvents.forEach {
      remandCalculation.issuesWithLegacyData.add(LegacyDataProblem(LegacyDataProblemType.MISSING_RECALL_EVENT, "The offence '${it.charge.offence.description}' within booking ${it.charge.bookNumber} does not have a recall court event.", it.charge))
    }
  }
}
