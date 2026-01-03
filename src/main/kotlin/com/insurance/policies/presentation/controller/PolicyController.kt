package com.insurance.policies.presentation.controller

import com.insurance.policies.application.dto.PolicyAtDateResponse
import com.insurance.policies.application.dto.PolicyResponse
import com.insurance.policies.application.service.InsuranceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/policies")
class PolicyController(
    private val insuranceService: InsuranceService
) {

    /**
     * Gets all policies for a personal number that are active on a specific date.
     *
     * GET /api/v1/policies?personalNumber=199001011234&date=2024-01-01
     */
    @GetMapping
    fun getPoliciesAtDate(
        @RequestParam personalNumber: String,
        @RequestParam date: LocalDate
    ): ResponseEntity<PolicyAtDateResponse> {
        val response = insuranceService.getPoliciesAtDate(personalNumber, date)
        return ResponseEntity.ok(response)
    }

    /**
     * Gets a specific policy by ID.
     *
     * GET /api/v1/policies/{id}
     */
    @GetMapping("/{id}")
    fun getPolicy(@PathVariable id: UUID): ResponseEntity<PolicyResponse> {
        val response = insuranceService.getPolicy(id)
        return ResponseEntity.ok(response)
    }
}
