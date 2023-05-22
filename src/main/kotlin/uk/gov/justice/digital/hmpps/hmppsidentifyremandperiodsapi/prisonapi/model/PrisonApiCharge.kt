package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model

import java.time.LocalDate

data class PrisonApiCharge(
  val chargeId: Long,
  val offenceCode: String,
  val offenceStatue: String,
  val offenceDate: LocalDate? = null,
  val offenceEndDate: LocalDate? = null,
  val offenceDescription: String,
  val guilty: Boolean,
  val courtCaseId: Long,
  val courtCaseRef: String? = null,
  val courtLocation: String? = null,
  val sentenceSequence: Int? = null,
  val sentenceDate: LocalDate? = null,
  val resultDescription: String? = null,
)
