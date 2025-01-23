package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
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

  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  val options: JsonNode? = null,
)
