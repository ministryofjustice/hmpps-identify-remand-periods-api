package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class ThingsToDo(
  val prisonerId: String,
  val thingsToDo: List<ToDoType> = emptyList(),
  val days: Int? = null,
)
enum class ToDoType {
  IDENTIFY_REMAND_REVIEW_FIRST_TIME,
  IDENTIFY_REMAND_REVIEW_FIRST_TIME_UPGRADE_DOWNGRADE,
  IDENTIFY_REMAND_REVIEW_UPDATE,
}
