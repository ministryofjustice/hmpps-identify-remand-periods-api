package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

@Entity
@Table
data class CalculatedRemand(
  @Id
  val id: UUID = UUID.randomUUID(),

  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "identifyRemandCalculationId", nullable = false, updatable = false)
  var identifyRemandCalculation: IdentifyRemandCalculation = IdentifyRemandCalculation(),

  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val statuses: String? = null,

)
