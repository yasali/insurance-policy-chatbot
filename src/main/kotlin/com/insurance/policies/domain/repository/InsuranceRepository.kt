package com.insurance.policies.domain.repository

import com.insurance.policies.domain.model.Insurance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface InsuranceRepository : JpaRepository<Insurance, UUID> {

    /**
     * Finds an insurance by personal number.
     */
    fun findByPersonalNumber(personalNumber: String): Insurance?

    /**
     * Checks if an insurance exists for a given personal number.
     */
    fun existsByPersonalNumber(personalNumber: String): Boolean

    /**
     * Finds insurance with all policies eagerly loaded.
     * Useful when we know we'll need the policies to avoid N+1 queries.
     */
    @Query("""
        SELECT DISTINCT i FROM Insurance i
        LEFT JOIN FETCH i._policies
        WHERE i.id = :id
    """)
    fun findByIdWithPolicies(@Param("id") id: UUID): Insurance?

    /**
     * Finds insurance by personal number with policies eagerly loaded.
     */
    @Query("""
        SELECT DISTINCT i FROM Insurance i
        LEFT JOIN FETCH i._policies
        WHERE i.personalNumber = :personalNumber
    """)
    fun findByPersonalNumberWithPolicies(@Param("personalNumber") personalNumber: String): Insurance?
}
