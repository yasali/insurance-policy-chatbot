package com.insurance.policies.presentation.controller

import com.insurance.policies.application.dto.ClaimResponse
import com.insurance.policies.application.dto.SubmitClaimRequest
import com.insurance.policies.application.service.ClaimExtractionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/claims")
class ClaimController(
    private val claimService: ClaimExtractionService
) {

    /**
     * Submits a new claim with automatic data extraction.
     *
     * POST /api/v1/claims
     */
    @PostMapping
    fun submitClaim(@Valid @RequestBody request: SubmitClaimRequest): ResponseEntity<ClaimResponse> {
        val response = claimService.submitClaim(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Gets a claim by ID.
     *
     * GET /api/v1/claims/{id}
     */
    @GetMapping("/{id}")
    fun getClaim(@PathVariable id: UUID): ResponseEntity<ClaimResponse> {
        val response = claimService.getClaim(id)
        return ResponseEntity.ok(response)
    }
}
