package com.insurance.policies.application.service

import com.insurance.policies.application.dto.*
import com.insurance.policies.domain.model.Insurance
import com.insurance.policies.domain.model.Policy
import com.insurance.policies.domain.repository.InsuranceRepository
import com.insurance.policies.domain.repository.PolicyRepository
import com.insurance.policies.infrastructure.exception.DuplicateInsuranceException
import com.insurance.policies.infrastructure.exception.InsuranceNotFoundException
import com.insurance.policies.infrastructure.exception.PolicyNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class InsuranceService(
    private val insuranceRepository: InsuranceRepository,
    private val policyRepository: PolicyRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Creates a new insurance with an initial policy.
     */
    fun createInsurance(request: CreateInsuranceRequest): InsuranceResponse {
        logger.info("Creating insurance for personal number: {}", request.personalNumber)

        // Check if insurance already exists
        if (insuranceRepository.existsByPersonalNumber(request.personalNumber)) {
            throw DuplicateInsuranceException(request.personalNumber)
        }

        // Create insurance
        val insurance = Insurance(personalNumber = request.personalNumber)

        // Create initial policy
        val policy = Policy(
            address = request.address,
            postalCode = request.postalCode,
            startDate = request.startDate
        )

        // Add policy to insurance
        insurance.addPolicy(policy)

        // Save
        val saved = insuranceRepository.save(insurance)

        logger.info("Insurance created successfully: {}", saved.id)
        return InsuranceResponse.from(saved)
    }

    /**
     * Adds a new policy to an existing insurance.
     */
    fun addPolicy(insuranceId: UUID, request: AddPolicyRequest): InsuranceResponse {
        logger.info("Adding policy to insurance: {}", insuranceId)

        // Load insurance with policies
        val insurance = insuranceRepository.findByIdWithPolicies(insuranceId)
            ?: throw InsuranceNotFoundException(insuranceId.toString())

        // Create new policy
        val policy = Policy(
            address = request.address,
            postalCode = request.postalCode,
            startDate = request.startDate
        )

        // Add policy (validates timeline)
        insurance.addPolicy(policy)

        // Save
        val saved = insuranceRepository.save(insurance)

        logger.info("Policy added successfully: {}", policy.id)
        return InsuranceResponse.from(saved)
    }

    /**
     * Gets an insurance by ID.
     */
    @Transactional(readOnly = true)
    fun getInsurance(insuranceId: UUID): InsuranceResponse {
        val insurance = insuranceRepository.findByIdWithPolicies(insuranceId)
            ?: throw InsuranceNotFoundException(insuranceId.toString())

        return InsuranceResponse.from(insurance)
    }

    /**
     * Gets an insurance by personal number.
     */
    @Transactional(readOnly = true)
    fun getInsuranceByPersonalNumber(personalNumber: String): InsuranceResponse {
        val insurance = insuranceRepository.findByPersonalNumberWithPolicies(personalNumber)
            ?: throw InsuranceNotFoundException(personalNumber)

        return InsuranceResponse.from(insurance)
    }

    /**
     * Gets all policies for a personal number that are active on a specific date.
     */
    @Transactional(readOnly = true)
    fun getPoliciesAtDate(personalNumber: String, date: LocalDate): PolicyAtDateResponse {        
        val policies = policyRepository.findByPersonalNumberAndDate(personalNumber, date)

        return PolicyAtDateResponse(
            personalNumber = personalNumber,
            queryDate = date,
            policies = policies.map { PolicyResponse.from(it) }
        )
    }

    /**
     * Gets a specific policy by ID.
     */
    @Transactional(readOnly = true)
    fun getPolicy(policyId: UUID): PolicyResponse {
        val policy = policyRepository.findById(policyId)
            .orElseThrow { PolicyNotFoundException(policyId) }

        return PolicyResponse.from(policy)
    }

    /**
     * Gets the policy for an insurance at a specific date.
     */
    @Transactional(readOnly = true)
    fun getPolicyForInsuranceAtDate(insuranceId: UUID, date: LocalDate): PolicyResponse? {
        val policy = policyRepository.findByInsuranceIdAndDate(insuranceId, date)
            ?: return null

        return PolicyResponse.from(policy)
    }
}
