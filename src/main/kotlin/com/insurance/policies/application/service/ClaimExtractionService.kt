package com.insurance.policies.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.insurance.policies.application.dto.ClaimResponse
import com.insurance.policies.application.dto.SubmitClaimRequest
import com.insurance.policies.domain.model.Claim
import com.insurance.policies.domain.model.ClaimData
import com.insurance.policies.domain.repository.ClaimRepository
import com.insurance.policies.domain.repository.PolicyRepository
import com.insurance.policies.infrastructure.exception.LLMResponseParsingException
import com.insurance.policies.infrastructure.exception.PolicyNotFoundException
import com.insurance.policies.infrastructure.llm.buildChatMessages
import com.insurance.policies.infrastructure.llm.LLMProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Claim extraction service using LLM for structured data extraction.
 */
@Service
@Transactional
class ClaimExtractionService(
    private val llmProvider: LLMProvider,
    private val claimRepository: ClaimRepository,
    private val policyRepository: PolicyRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Submits a claim and extracts structured data from unstructured text.
     */
    fun submitClaim(request: SubmitClaimRequest): ClaimResponse {
        logger.info("Submitting claim for policy: {}", request.policyId)

        // Validate policy exists
        val policy = policyRepository.findById(request.policyId)
            .orElseThrow { PolicyNotFoundException(request.policyId) }

        // Create claim
        val claim = Claim(
            policy = policy,
            rawText = request.claimText
        )

        // Save initially as PENDING
        val savedClaim = claimRepository.save(claim)

        // Extract data asynchronously (in production, this could be async)
        try {
            val extractionResult = extractClaimData(request.claimText)
            savedClaim.extract(extractionResult.data, extractionResult.confidence)
            claimRepository.save(savedClaim)

            logger.info("Claim extraction completed: id={}, confidence={}", savedClaim.id, extractionResult.confidence)
        } catch (ex: Exception) {
            logger.error("Claim extraction failed for claim: {}", savedClaim.id, ex)
            // Claim remains in PENDING status
        }

        return ClaimResponse.from(savedClaim)
    }

    /**
     * Gets a claim by ID.
     */
    @Transactional(readOnly = true)
    fun getClaim(claimId: UUID): ClaimResponse {
        val claim = claimRepository.findById(claimId)
            .orElseThrow { throw IllegalArgumentException("Claim not found: $claimId") }

        return ClaimResponse.from(claim)
    }

    /**
     * Extracts structured data from claim text using LLM.
     */
    private fun extractClaimData(claimText: String): ExtractionResult {
        val schema = """
        {
          "damageType": "string (e.g., 'water damage', 'theft', 'fire', 'accidental damage')",
          "location": "string (where the incident occurred, e.g., 'kitchen', 'bedroom', 'home')",
          "incidentDate": "string (ISO date YYYY-MM-DD or null if not mentioned)",
          "estimatedCost": "number (cost in SEK or null if not mentioned)",
          "description": "string (brief summary of the incident)",
          "additionalDetails": "object (OPTIONAL - any other relevant information as key-value pairs, omit if nothing relevant)"
        }
        """.trimIndent()

        val messages = buildChatMessages {
            system(buildExtractionPrompt(schema))
            user(claimText)
        }

        val jsonResponse = llmProvider.chatWithJson(messages, schema)

        return try {
            // Parse JSON response
            val extractedData = objectMapper.readValue<ClaimData>(jsonResponse)

            // Calculate confidence
            val confidence = calculateConfidence(extractedData, claimText)

            ExtractionResult(extractedData, confidence)
        } catch (ex: Exception) {
            throw LLMResponseParsingException("Failed to parse claim extraction response: ${ex.message}", ex)
        }
    }

    /**
     * Builds the extraction prompt.
     */
    private fun buildExtractionPrompt(schema: String): String {
        return """
        You are an insurance claim data extraction system.

        Extract structured information from the claim text provided by the user.

        INSTRUCTIONS:
        1. Extract only information that is explicitly mentioned in the text
        2. Do NOT make assumptions or invent information
        3. Use null for fields where information is not available
        4. For damageType, categorize as one of: water damage, fire damage, theft, burglary, accidental damage, storm damage, or other
        5. For location, extract where the incident happened (kitchen, bedroom, living room, etc.)
        6. For incidentDate, extract dates and convert to ISO format (YYYY-MM-DD). Use null if no date is mentioned.
        7. For estimatedCost, extract only if explicitly mentioned. Use null otherwise.
        8. Provide a brief description summarizing the incident

        Expected JSON Schema:
        $schema

        IMPORTANT: Respond with ONLY valid JSON. No markdown, no explanations.
        """.trimIndent()
    }

    /**
     * Calculates confidence score for the extraction.
     */
    private fun calculateConfidence(data: ClaimData, rawText: String): Double {
        var score = 0.0
        val rawLower = rawText.lowercase()

        // Field completeness (40%)
        var fieldsPresent = 0
        var totalFields = 5

        if (data.damageType.isNotBlank()) fieldsPresent++
        if (data.location.isNotBlank()) fieldsPresent++
        if (data.incidentDate != null) fieldsPresent++
        if (data.estimatedCost != null) fieldsPresent++
        if (data.description.isNotBlank()) fieldsPresent++

        score += (fieldsPresent.toDouble() / totalFields) * 0.4

        // Entity presence in text (30%)
        var entitiesFound = 0
        var totalEntities = 0

        // Check if damage type appears in text
        totalEntities++
        if (rawLower.contains(data.damageType.lowercase())) entitiesFound++

        // Check if location appears in text
        totalEntities++
        if (rawLower.contains(data.location.lowercase())) entitiesFound++

        if (totalEntities > 0) {
            score += (entitiesFound.toDouble() / totalEntities) * 0.3
        }

        // Date validity (15%)
        if (data.incidentDate != null) {
            score += 0.15
        }

        // Description quality (15%)
        if (data.description.split(" ").size >= 5) {
            score += 0.15
        }

        return score.coerceIn(0.0, 1.0)
    }

    private data class ExtractionResult(
        val data: ClaimData,
        val confidence: Double
    )
}
