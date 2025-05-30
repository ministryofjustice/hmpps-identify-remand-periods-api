package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.transform

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model.PrisonApiCourtDateOutcome
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeLegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.CONTINUE
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.START
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.STOP
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblem
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.LegacyDataProblemType
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.mojDisplayFormat

const val RECALL_COURT_EVENT = "1501"
fun mapCourtDateResult(courtDateResult: PrisonApiCourtDateOutcome, charge: PrisonApiCharge, issuesWithLegacyData: MutableList<LegacyDataProblem>): CourtDateType? = when (courtDateResult.resultCode) {
  "4531" -> START
  "4560" -> START
  "4565" -> START
  "4004" -> START
  "4016" -> START
  "4001" -> START
  "4012" -> START
  "4588" -> START
  "4534" -> START
  "4537" -> START
  "2053" -> STOP
  RECALL_COURT_EVENT -> STOP
  "1115" -> STOP
  "1116" -> STOP
  "4530" -> STOP
  "1002" -> STOP
  "4015" -> STOP
  "1510" -> STOP
  "2006" -> STOP
  "2005" -> STOP
  "2004" -> STOP
  "4542" -> STOP
  "1024" -> STOP
  "1007" -> STOP
  "4559" -> STOP
  "1015" -> STOP
  "2050" -> STOP
  "1507" -> STOP
  "1106" -> STOP
  "1057" -> STOP
  "5600" -> STOP
  "1081" -> STOP
  "1018" -> STOP
  "1105" -> STOP
  "4529" -> STOP
  "2051" -> STOP
  "1019" -> STOP
  "1022" -> STOP
  "3012" -> STOP
  "3058" -> STOP
  "1017" -> STOP
  "4543" -> STOP
  "4558" -> STOP
  "4014" -> STOP
  "2068" -> STOP
  "2066" -> STOP
  "4589" -> STOP
  "2067" -> STOP
  "2065" -> STOP
  "NC" -> STOP
  "2501" -> STOP
  "4572" -> STOP
  "4506" -> CONTINUE
  "5500" -> CONTINUE
  "G" -> CONTINUE
  "1046" -> CONTINUE
  "3045" -> CONTINUE
  "2007" -> CONTINUE
  "3044" -> CONTINUE
  "3056" -> CONTINUE
  "1029" -> CONTINUE
  "1021" -> CONTINUE
  "5502" -> CONTINUE
  "5501" -> CONTINUE
  "1003" -> STOP
  "1004" -> STOP
  "1008" -> STOP
  "1009" -> STOP
  "1010" -> STOP
  "1012" -> STOP
  "1013" -> STOP
  "1016" -> STOP
  "1028" -> STOP
  "1032" -> STOP
  "1040" -> STOP
  "1087" -> STOP
  "1089" -> STOP
  "1097" -> STOP
  "1102" -> STOP
  "1103" -> STOP
  "1108" -> STOP
  "1109" -> STOP
  "1110" -> STOP
  "1111" -> STOP
  "1113" -> STOP
  "1114" -> STOP
  "1508" -> STOP
  "1509" -> STOP
  "2008" -> STOP
  "2009" -> STOP
  "2052" -> STOP
  "2060" -> STOP
  "2061" -> STOP
  "2063" -> STOP
  "2064" -> STOP
  "2507" -> CONTINUE
  "2511" -> CONTINUE
  "2514" -> CONTINUE
  "3006" -> STOP
  "3007" -> CONTINUE
  "3008" -> CONTINUE
  "3011" -> CONTINUE
  "3019" -> STOP
  "3042" -> CONTINUE
  "3047" -> STOP
  "3054" -> CONTINUE
  "3063" -> STOP
  "3067" -> STOP
  "3070" -> CONTINUE
  "3072" -> CONTINUE
  "3080" -> STOP
  "3081" -> STOP
  "3083" -> CONTINUE
  "3086" -> STOP
  "3088" -> STOP
  "3091" -> CONTINUE
  "3096" -> CONTINUE
  "3101" -> STOP
  "3102" -> STOP
  "3105" -> STOP
  "3107" -> STOP
  "3108" -> STOP
  "3109" -> STOP
  "3112" -> STOP
  "3501" -> STOP
  "3502" -> STOP
  "4005" -> CONTINUE
  "4007" -> CONTINUE
  "4008" -> CONTINUE
  "4010" -> STOP
  "4011" -> STOP
  "4013" -> STOP
  "4017" -> STOP
  "4018" -> STOP
  "4020" -> STOP
  "4021" -> STOP
  "4022" -> STOP
  "4508" -> CONTINUE
  "4509" -> CONTINUE
  "4532" -> START
  "4533" -> STOP
  "4535" -> START
  "4536" -> START
  "4538" -> STOP
  "4539" -> START
  "4541" -> CONTINUE
  "4548" -> STOP
  "4550" -> STOP
  "4552" -> STOP
  "4553" -> START
  "4554" -> CONTINUE
  "4555" -> STOP
  "4561" -> START
  "4562" -> STOP
  "4563" -> START
  "4564" -> START
  "4570" -> START
  "4571" -> START
  "4573" -> CONTINUE
  "4575" -> STOP
  "4576" -> START
  "4577" -> STOP
  "4582" -> STOP
  "4584" -> CONTINUE
  "4587" -> STOP
  "4590" -> CONTINUE
  "5601" -> CONTINUE
  "5602" -> STOP
  "FPR" -> STOP

  else -> {
    issuesWithLegacyData.add(ChargeLegacyDataProblem(LegacyDataProblemType.UNSUPPORTED_OUTCOME, "The court event on ${courtDateResult.date.mojDisplayFormat()} for offence ${charge.offenceDescription} committed at ${charge.offenceDate?.mojDisplayFormat() ?: "Unknown"} has an unsupported outcome ${courtDateResult.resultCode}: ${courtDateResult.resultDescription}", charge))
    null
  }
}

val CUSTODIAL_EVENTS = listOf("1002", "1501", "1510", "1024", "1007", "1081", "1509", "1022", "3058", "1110", "1003", "1111")

fun isEventCustodial(courtDateResult: PrisonApiCourtDateOutcome): Boolean = CUSTODIAL_EVENTS.contains(courtDateResult.resultCode)
