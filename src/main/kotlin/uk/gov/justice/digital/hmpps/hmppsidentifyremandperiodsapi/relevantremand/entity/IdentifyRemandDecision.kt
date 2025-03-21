package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID
@Entity
@Table
data class IdentifyRemandDecision(
  @Id
  val id: UUID = UUID.randomUUID(),

  val accepted: Boolean = false,

  val days: Int = 0,

  val person: String = "",

  val rejectComment: String? = null,

  val decisionAt: LocalDateTime = LocalDateTime.now(),

  val decisionByUsername: String = "",

  val decisionByPrisonId: String? = null,

  @OneToOne(mappedBy = "identifyRemandDecision", cascade = [CascadeType.ALL])
  var identifyRemandCalculation: IdentifyRemandCalculation?,

) {
  init {
    identifyRemandCalculation?.identifyRemandDecision = this
  }
}
