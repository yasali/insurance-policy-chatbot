package com.insurance.policies.application.dto

import com.insurance.policies.domain.model.Insurance
import com.insurance.policies.domain.model.Policy
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Request to create a new insurance with an initial policy.
 */
data class CreateInsuranceRequest(
    @field:NotBlank(message = "Personal number is required")
    val personalNumber: String,

    @field:NotBlank(message = "Address is required")
    @field:Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters")
    val address: String,

    @field:NotBlank(message = "Postal code is required")
    @field:Pattern(
        regexp = "^\\d{5}$",
        message = "Postal code must be 5 digits"
    )
    val postalCode: String,

    val startDate: LocalDate = LocalDate.now()
)

/**
 * Request to add a new policy to an existing insurance.
 */
data class AddPolicyRequest(
    @field:NotBlank(message = "Address is required")
    @field:Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters")
    val address: String,

    @field:NotBlank(message = "Postal code is required")
    @field:Pattern(
        regexp = "^\\d{5}$",
        message = "Postal code must be 5 digits"
    )
    val postalCode: String,

    val startDate: LocalDate
)

/**
 * Response representing an insurance with its policies.
 */
data class InsuranceResponse(
    val id: UUID,
    val personalNumber: String,
    val policies: List<PolicyResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(insurance: Insurance): InsuranceResponse {
            return InsuranceResponse(
                id = insurance.id,
                personalNumber = insurance.personalNumber,
                policies = insurance.policies.map { PolicyResponse.from(it) },
                createdAt = insurance.createdAt,
                updatedAt = insurance.updatedAt
            )
        }
    }
}

/**
 * Response representing a single policy.
 */
data class PolicyResponse(
    val id: UUID,
    val address: String,
    val postalCode: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(policy: Policy): PolicyResponse {
            return PolicyResponse(
                id = policy.id,
                address = policy.address,
                postalCode = policy.postalCode,
                startDate = policy.startDate,
                endDate = policy.endDate,
                isActive = policy.isCurrent(),
                createdAt = policy.createdAt,
                updatedAt = policy.updatedAt
            )
        }
    }
}

/**
 * Response for policy queries at specific dates.
 */
data class PolicyAtDateResponse(
    val personalNumber: String,
    val queryDate: LocalDate,
    val policies: List<PolicyResponse>
)
