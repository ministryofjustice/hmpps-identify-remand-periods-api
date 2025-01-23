package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
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

  @JdbcTypeCode(SqlTypes.JSON)
  val options: RemandCalculationRequestOptions? = null,
)
