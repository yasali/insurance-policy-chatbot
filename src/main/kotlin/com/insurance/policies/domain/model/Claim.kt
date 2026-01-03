package com.insurance.policies.domain.model

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "claim")
class Claim(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    val policy: Policy,

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    val rawText: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_data", columnDefinition = "JSON")
    var extractedData: ClaimData? = null,

    @Column(name = "confidence")
    var confidence: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ClaimStatus = ClaimStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun extract(data: ClaimData, confidence: Double) {
        require(confidence in 0.0..1.0) { "Confidence must be between 0.0 and 1.0" }

        this.extractedData = data
        this.confidence = confidence
        this.status = ClaimStatus.EXTRACTED
        this.updatedAt = Instant.now()
    }

    fun validate() {
        require(status == ClaimStatus.EXTRACTED) { "Claim must be extracted before validation" }
        this.status = ClaimStatus.VALIDATED
        this.updatedAt = Instant.now()
    }

    fun reject() {
        this.status = ClaimStatus.REJECTED
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Claim) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ClaimData(
    val damageType: String,
    val location: String,
    val incidentDate: LocalDate? = null,
    val estimatedCost: Double? = null,
    val description: String,
    val additionalDetails: Map<String, Any>? = null
) {
    init {
        require(damageType.isNotBlank()) { "Damage type cannot be blank" }
        require(location.isNotBlank()) { "Location cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        estimatedCost?.let {
            require(it >= 0) { "Estimated cost cannot be negative" }
        }
    }
}

enum class ClaimStatus {
    PENDING,
    EXTRACTED,
    VALIDATED,
    REJECTED
}
