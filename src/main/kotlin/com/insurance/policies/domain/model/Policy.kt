package com.insurance.policies.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(
    name = "policy",
    indexes = [
        Index(name = "idx_policy_insurance_id", columnList = "insurance_id"),
        Index(name = "idx_policy_start_date", columnList = "start_date"),
        Index(name = "idx_policy_date_range", columnList = "start_date,end_date")
    ]
)
class Policy(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insurance_id", nullable = false)
    var insurance: Insurance? = null,

    @Column(name = "address", nullable = false)
    var address: String,

    @Column(name = "postal_code", nullable = false, length = 10)
    var postalCode: String,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = true)
    var endDate: LocalDate? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * Checks if this policy is active at a given date.
     */
    fun isActiveAt(date: LocalDate): Boolean {
        val policyEndDate = endDate
        return startDate <= date && (policyEndDate == null || policyEndDate >= date)
    }

    /**
     * Checks if this policy is currently active (no end date).
     */
    fun isCurrent(): Boolean {
        return endDate == null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Policy) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Policy(id=$id, address='$address', postalCode='$postalCode', " +
                "startDate=$startDate, endDate=$endDate)"
    }
}
