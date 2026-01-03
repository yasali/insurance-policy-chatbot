package com.insurance.policies.infrastructure.llm

/**
 * Abstraction for LLM providers. pluggable LLM backends (Ollama, OpenAI, Gemini, etc.)
 */
interface LLMProvider {

    /**
     * Sends a chat request and returns the response.
     *
     * @param messages The conversation messages
     * @param temperature Sampling temperature (0.0 = deterministic, 1.0 = creative)
     * @return The LLM's response text
     */
    fun chat(messages: List<ChatMessage>, temperature: Double = 0.7): String

    /**
     * Sends a chat request expecting JSON response.
     *
     * @param messages The conversation messages
     * @param jsonSchema Optional schema hint for the JSON structure
     * @return The LLM's response as JSON string
     */
    fun chatWithJson(messages: List<ChatMessage>, jsonSchema: String? = null): String

    /**
     * Gets the provider name.
     */
    fun getProviderName(): String

    /**
     * Gets the model being used.
     */
    fun getModelName(): String

    /**
     * Health check for the provider.
     */
    fun isHealthy(): Boolean
}

/**
 * Represents a message in a conversation.
 */
data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
}

/**
 * Response from an LLM call
 */
data class LLMResponse(
    val content: String,
    val model: String,
    val provider: String,
    val tokensUsed: Int? = null,
    val durationMs: Long
)

/**
 * Builder for constructing chat messages.
 */
class ChatMessageBuilder {
    private val messages = mutableListOf<ChatMessage>()

    fun system(content: String) = apply {
        messages.add(ChatMessage(ChatMessage.Role.SYSTEM, content))
    }

    fun user(content: String) = apply {
        messages.add(ChatMessage(ChatMessage.Role.USER, content))
    }

    fun assistant(content: String) = apply {
        messages.add(ChatMessage(ChatMessage.Role.ASSISTANT, content))
    }

    fun build(): List<ChatMessage> = messages.toList()
}

/**
 * Builds chat messages.
 */
fun buildChatMessages(init: ChatMessageBuilder.() -> Unit): List<ChatMessage> {
    return ChatMessageBuilder().apply(init).build()
}
