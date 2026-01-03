package com.insurance.policies.presentation.controller

import com.insurance.policies.application.dto.*
import com.insurance.policies.application.service.InsuranceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/insurances")
class InsuranceController(
    private val insuranceService: InsuranceService
) {

    /**
     * Creates a new insurance with an initial policy.
     *
     * POST /api/v1/insurances
     */
    @PostMapping
    fun createInsurance(
        @Valid @RequestBody request: CreateInsuranceRequest
    ): ResponseEntity<InsuranceResponse> {
        val response = insuranceService.createInsurance(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Gets an insurance by ID.
     *
     * GET /api/v1/insurances/{id}
     */
    @GetMapping("/{id}")
    fun getInsurance(@PathVariable id: UUID): ResponseEntity<InsuranceResponse> {
        val response = insuranceService.getInsurance(id)
        return ResponseEntity.ok(response)
    }

    /**
     * Adds a new policy to an existing insurance.
     *
     * POST /api/v1/insurances/{id}/policies
     */
    @PostMapping("/{id}/policies")
    fun addPolicy(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddPolicyRequest
    ): ResponseEntity<InsuranceResponse> {
        val response = insuranceService.addPolicy(id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Gets the policy for an insurance at a specific date.
     *
     * GET /api/v1/insurances/{id}/policies?date=2024-01-01
     */
    @GetMapping("/{id}/policies")
    fun getPolicyAtDate(
        @PathVariable id: UUID,
        @RequestParam date: LocalDate
    ): ResponseEntity<PolicyResponse> {
        val response = insuranceService.getPolicyForInsuranceAtDate(id, date)
        return response?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
