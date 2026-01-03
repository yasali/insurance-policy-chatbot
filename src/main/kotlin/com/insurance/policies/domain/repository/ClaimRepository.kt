package com.insurance.policies.domain.repository

import com.insurance.policies.domain.model.Claim
import com.insurance.policies.domain.model.ClaimStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ClaimRepository : JpaRepository<Claim, UUID> {

    /**
     * Finds all claims for a given policy.
     */
    fun findByPolicy_Id(policyId: UUID): List<Claim>

    /**
     * Finds claims by status.
     */
    fun findByStatus(status: ClaimStatus): List<Claim>

    /**
     * Finds all claims for a given insurance (across all policies).
     */
    @Query("""
        SELECT c FROM Claim c
        JOIN c.policy p
        WHERE p.insurance.id = :insuranceId
        ORDER BY c.createdAt DESC
    """)
    fun findByInsuranceId(@Param("insuranceId") insuranceId: UUID): List<Claim>
}
