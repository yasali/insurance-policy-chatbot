package com.insurance.policies.application.dto

import com.insurance.policies.domain.model.Conversation
import com.insurance.policies.domain.model.ConversationMessage
import com.insurance.policies.domain.model.MessageRole
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.*

/**
 * Request to send a message to the policy assistant.
 */
data class ChatRequest(
    @field:NotBlank(message = "Personal number is required")
    val personalNumber: String,

    @field:NotBlank(message = "Message is required")
    val message: String,

    val conversationId: UUID? = null
)

/**
 * Response from the policy assistant.
 */
data class ChatResponse(
    val conversationId: UUID,
    val message: String,
    val timestamp: Instant
)

/**
 * Response representing a conversation with its messages.
 */
data class ConversationResponse(
    val id: UUID,
    val insuranceId: UUID,
    val messages: List<MessageResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(conversation: Conversation): ConversationResponse {
            return ConversationResponse(
                id = conversation.id,
                insuranceId = conversation.insurance.id,
                messages = conversation.messages.map { MessageResponse.from(it) },
                createdAt = conversation.createdAt,
                updatedAt = conversation.updatedAt
            )
        }
    }
}

/**
 * Response representing a message in a conversation.
 */
data class MessageResponse(
    val id: UUID,
    val role: MessageRole,
    val content: String,
    val createdAt: Instant
) {
    companion object {
        fun from(message: ConversationMessage): MessageResponse {
            return MessageResponse(
                id = message.id,
                role = message.role,
                content = message.content,
                createdAt = message.createdAt
            )
        }
    }
}
