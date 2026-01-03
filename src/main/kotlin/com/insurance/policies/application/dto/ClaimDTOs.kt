package com.insurance.policies.application.dto

import com.insurance.policies.domain.model.Claim
import com.insurance.policies.domain.model.ClaimData
import com.insurance.policies.domain.model.ClaimStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Request to submit a new claim.
 */
data class SubmitClaimRequest(
    val policyId: UUID,

    @field:NotBlank(message = "Claim text is required")
    val claimText: String
)

/**
 * Response for a claim with extraction results.
 */
data class ClaimResponse(
    val id: UUID,
    val policyId: UUID,
    val rawText: String,
    val extractedData: ClaimDataResponse?,
    val confidence: Double?,
    val status: ClaimStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(claim: Claim): ClaimResponse {
            return ClaimResponse(
                id = claim.id,
                policyId = claim.policy.id,
                rawText = claim.rawText,
                extractedData = claim.extractedData?.let { ClaimDataResponse.from(it) },
                confidence = claim.confidence,
                status = claim.status,
                createdAt = claim.createdAt,
                updatedAt = claim.updatedAt
            )
        }
    }
}

/**
 * Response for extracted claim data.
 */
data class ClaimDataResponse(
    val damageType: String,
    val location: String,
    val incidentDate: LocalDate?,
    val estimatedCost: Double?,
    val description: String,
    val additionalDetails: Map<String, Any>?
) {
    companion object {
        fun from(data: ClaimData): ClaimDataResponse {
            return ClaimDataResponse(
                damageType = data.damageType,
                location = data.location,
                incidentDate = data.incidentDate,
                estimatedCost = data.estimatedCost,
                description = data.description,
                additionalDetails = data.additionalDetails
            )
        }
    }
}
