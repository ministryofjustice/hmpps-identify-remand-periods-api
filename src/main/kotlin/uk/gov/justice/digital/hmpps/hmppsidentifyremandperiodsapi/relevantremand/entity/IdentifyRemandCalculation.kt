package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID
@Entity
@Table
data class IdentifyRemandCalculation(
  @Id
  val id: UUID = UUID.randomUUID(),

  @NotNull
  @OneToOne(optional = false)
  @JoinColumn(name = "identifyRemandDecisionId", nullable = false, updatable = false)
  var identifyRemandDecision: IdentifyRemandDecision? = null,

  @JdbcTypeCode(SqlTypes.JSON)
  val inputData: JsonNode? = null,

  @JdbcTypeCode(SqlTypes.JSON)
  val outputData: JsonNode? = null,

  @OneToMany(mappedBy = "identifyRemandCalculation", cascade = [CascadeType.ALL])
  @JsonIgnore
  var calculatedRemand: List<CalculatedRemand> = ArrayList(),

  @JdbcTypeCode(SqlTypes.JSON)
  val options: JsonNode? = null,
) {

  init {
    calculatedRemand.forEach {
      it.identifyRemandCalculation = this
    }
  }
}
