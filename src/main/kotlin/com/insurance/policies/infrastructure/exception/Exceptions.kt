package com.insurance.policies.infrastructure.exception

import java.time.LocalDate
import java.util.*

/**
 * Base exception for all domain exceptions.
 */
sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when insurance operations fail.
 */
sealed class InsuranceException(message: String, cause: Throwable? = null) : DomainException(message, cause)

class InsuranceNotFoundException(personalNumber: String) :
    InsuranceException("Insurance not found for personal number: $personalNumber")

class DuplicateInsuranceException(personalNumber: String) :
    InsuranceException("Insurance already exists for personal number: $personalNumber")

/**
 * Thrown when policy operations fail.
 */
sealed class PolicyException(message: String, cause: Throwable? = null) : DomainException(message, cause)

class PolicyNotFoundException(policyId: UUID) :
    PolicyException("Policy not found: $policyId")

/**
 * Thrown when conversation operations fail.
 */
sealed class ConversationException(message: String, cause: Throwable? = null) : DomainException(message, cause)

class ConversationNotFoundException(conversationId: UUID) :
    ConversationException("Conversation not found: $conversationId")

/**
 * Thrown when LLM operations fail.
 */
sealed class LLMException(message: String, cause: Throwable? = null) : DomainException(message, cause)

class LLMServiceUnavailableException(message: String, cause: Throwable? = null) :
    LLMException(message, cause)

class LLMResponseParsingException(message: String, cause: Throwable? = null) :
    LLMException(message, cause)

class LLMTimeoutException(message: String) :
    LLMException(message)
