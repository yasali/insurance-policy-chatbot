package com.insurance.policies.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "insurance")
class Insurance(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "personal_number", nullable = false, unique = true, length = 12)
    val personalNumber: String,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(
        mappedBy = "insurance",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private val _policies: MutableList<Policy> = mutableListOf()
) {
    val policies: List<Policy>
        get() = _policies.toList()

    /**
     * Adds a new policy to this insurance.
     * Automatically sets the end date of the previous policy.
     *
     * @throws IllegalStateException if the new policy's start date is not after the last policy
     */
    fun addPolicy(policy: Policy) {
        val lastPolicy = _policies.maxByOrNull { it.startDate }

        // Validate that new policy starts after the last one
        if (lastPolicy != null && policy.startDate <= lastPolicy.startDate) {
            throw IllegalStateException(
                "New policy must start after ${lastPolicy.startDate}, but was ${policy.startDate}"
            )
        }

        // Set end date of previous policy to day before new policy starts
        lastPolicy?.let {
            if (it.endDate == null) {
                it.endDate = policy.startDate.minusDays(1)
            }
        }

        policy.insurance = this
        _policies.add(policy)
        this.updatedAt = Instant.now()
    }

    /**
     * Gets the active policy at a specific date.
     *
     * @param date The date to query for
     * @return The policy active at the given date, or null if none exists
     */
    fun getPolicyAtDate(date: LocalDate): Policy? {
        return _policies.firstOrNull { policy ->
            val policyEndDate = policy.endDate
            policy.startDate <= date && (policyEndDate == null || policyEndDate >= date)
        }
    }

    /**
     * Gets the currently active policy (policy with no end date).
     *
     * @return The current policy, or null if none exists
     */
    fun getCurrentPolicy(): Policy? {
        return _policies.firstOrNull { it.endDate == null }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Insurance) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Insurance(id=$id, personalNumber='$personalNumber', policyCount=${_policies.size})"
    }
}
