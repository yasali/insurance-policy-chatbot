package com.insurance.policies.domain.repository

import com.insurance.policies.domain.model.Policy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface PolicyRepository : JpaRepository<Policy, UUID> {

    /**
     * Finds all policies for a given insurance.
     */
    fun findByInsurance_Id(insuranceId: UUID): List<Policy>

    /**
     * Finds policies by personal number that are active on a specific date.
     */
    @Query("""
        SELECT p FROM Policy p
        JOIN p.insurance i
        WHERE i.personalNumber = :personalNumber
        AND p.startDate <= :date
        AND (p.endDate IS NULL OR p.endDate >= :date)
    """)
    fun findByPersonalNumberAndDate(
        @Param("personalNumber") personalNumber: String,
        @Param("date") date: LocalDate
    ): List<Policy>

    /**
     * Finds the policy for a given insurance that is active on a specific date.
     */
    @Query("""
        SELECT p FROM Policy p
        WHERE p.insurance.id = :insuranceId
        AND p.startDate <= :date
        AND (p.endDate IS NULL OR p.endDate >= :date)
    """)
    fun findByInsuranceIdAndDate(
        @Param("insuranceId") insuranceId: UUID,
        @Param("date") date: LocalDate
    ): Policy?
}
