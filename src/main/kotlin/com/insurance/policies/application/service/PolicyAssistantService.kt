package com.insurance.policies.application.service

import com.insurance.policies.application.dto.ChatRequest
import com.insurance.policies.application.dto.ChatResponse
import com.insurance.policies.application.dto.ConversationResponse
import com.insurance.policies.domain.model.Conversation
import com.insurance.policies.domain.model.MessageRole
import com.insurance.policies.domain.model.Policy
import com.insurance.policies.domain.repository.ConversationRepository
import com.insurance.policies.domain.repository.InsuranceRepository
import com.insurance.policies.infrastructure.exception.ConversationNotFoundException
import com.insurance.policies.infrastructure.exception.InsuranceNotFoundException
import com.insurance.policies.infrastructure.llm.buildChatMessages
import com.insurance.policies.infrastructure.llm.LLMProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class PolicyAssistantService(
    private val llmProvider: LLMProvider,
    private val insuranceRepository: InsuranceRepository,
    private val conversationRepository: ConversationRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Sends a message to LLM and gets a response.
     */
    fun chat(request: ChatRequest): ChatResponse {
        logger.info("Processing chat request for personal number: {}", request.personalNumber)

        val insurance = insuranceRepository.findByPersonalNumberWithPolicies(request.personalNumber)
            ?: throw InsuranceNotFoundException(request.personalNumber)

        // Get or create conversation
        val conversation = if (request.conversationId != null) {
            conversationRepository.findByIdWithMessages(request.conversationId)
                ?: throw ConversationNotFoundException(request.conversationId)
        } else {
            // Create new conversation
            Conversation(insurance = insurance).also {
                conversationRepository.save(it)
            }
        }

        // Retrieve relevant policies
        val relevantPolicies = retrieveRelevantPolicies(insurance.policies, request.message)

        // Build context from policies
        val policyContext = buildPolicyContext(relevantPolicies)

        // Build conversation history
        val conversationHistory = buildConversationHistory(conversation)

        // Create prompt with context
        val messages = buildChatMessages {
            system(buildSystemPrompt(policyContext))

            // Add conversation history
            conversationHistory.forEach { msg ->
                when (msg.role) {
                    MessageRole.USER -> user(msg.content)
                    MessageRole.ASSISTANT -> assistant(msg.content)
                    else -> {}
                }
            }

            // Add current user message
            user(request.message)
        }

        // Call LLM
        val response = llmProvider.chat(messages, temperature = 0.7)

        // Store messages in conversation
        conversation.addMessage(MessageRole.USER, request.message)
        conversation.addMessage(MessageRole.ASSISTANT, response)
        conversationRepository.save(conversation)

        logger.info("Chat response generated for conversation: {}", conversation.id)

        return ChatResponse(
            conversationId = conversation.id,
            message = response,
            timestamp = Instant.now()
        )
    }

    /**
     * Gets a conversation by ID.
     */
    @Transactional(readOnly = true)
    fun getConversation(conversationId: UUID): ConversationResponse {
        val conversation = conversationRepository.findByIdWithMessages(conversationId)
            ?: throw ConversationNotFoundException(conversationId)

        return ConversationResponse.from(conversation)
    }

    /**
     * Retrieves relevant policies for the query.
     * Simple keyword based retrieval. Should be enhanced in future.
     */
    private fun retrieveRelevantPolicies(policies: List<Policy>, query: String): List<Policy> {
        val queryLower = query.lowercase()

        return when {
            // Specific date mentioned
            queryLower.contains("current") || queryLower.contains("now") -> {
                policies.filter { it.isCurrent() }
            }
            // Address query
            queryLower.contains("address") || queryLower.contains("location") -> {
                policies.take(3) // Return recent policies
            }
            // Coverage/start date query
            queryLower.contains("start") || queryLower.contains("when") -> {
                policies.filter { it.isCurrent() }.ifEmpty { policies.take(1) }
            }
            // Default: return current policy
            else -> {
                policies.filter { it.isCurrent() }.ifEmpty { policies.take(1) }
            }
        }.take(3) // Limit to 3 policies to avoid context overflow
    }

    /**
     * Builds policy context for the LLM.
     */
    private fun buildPolicyContext(policies: List<Policy>): String {
        if (policies.isEmpty()) {
            return "No policy information available."
        }

        return policies.joinToString("\n\n") { policy ->
            """
            Policy ID: ${policy.id}
            Address: ${policy.address}
            Postal Code: ${policy.postalCode}
            Coverage Start Date: ${policy.startDate}
            Coverage End Date: ${policy.endDate ?: "Active (no end date)"}
            Status: ${if (policy.isCurrent()) "Active" else "Inactive"}
            """.trimIndent()
        }
    }

    /**
     * Builds conversation history with sliding window.
     */
    private fun buildConversationHistory(conversation: Conversation) =
        conversation.getRecentMessages(maxMessages = 10)

    /**
     * Builds the system prompt with context.
     */
    private fun buildSystemPrompt(policyContext: String): String {
        return """
        You are a helpful insurance assistant for a home insurance company.

        CRITICAL RULES:
        1. Answer questions ONLY based on the policy information provided below
        2. If the answer is not in the policy information, say: "I don't have that information in your policy details. Please contact our support team."
        3. NEVER make up policy terms, coverage amounts, or dates
        4. Be concise and friendly
        5. Use the policy information literally - don't infer or assume

        POLICY INFORMATION:
        $policyContext

        Guidelines:
        - For address questions, provide the address and postal code
        - For coverage questions, mention the start date and status
        - For date questions, use the exact dates from the policy
        - If multiple policies exist, clarify which one you're referring to
        """.trimIndent()
    }
}
