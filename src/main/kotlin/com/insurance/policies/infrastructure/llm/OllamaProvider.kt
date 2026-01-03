package com.insurance.policies.infrastructure.llm

import com.fasterxml.jackson.annotation.JsonProperty
import com.insurance.policies.infrastructure.config.LLMConfig
import com.insurance.policies.infrastructure.exception.LLMResponseParsingException
import com.insurance.policies.infrastructure.exception.LLMServiceUnavailableException
import com.insurance.policies.infrastructure.exception.LLMTimeoutException
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Ollama LLM provider implementation.
 */
@Component
class OllamaProvider(
    private val config: LLMConfig,
    webClientBuilder: WebClient.Builder
) : LLMProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val webClient: WebClient = webClientBuilder
        .baseUrl(config.baseUrl)
        .build()

    override fun chat(messages: List<ChatMessage>, temperature: Double): String {
        val startTime = System.currentTimeMillis()

        try {
            val request = OllamaChatRequest(
                model = config.model,
                messages = messages.map { it.toOllamaMessage() },
                stream = false,
                options = OllamaOptions(temperature = temperature)
            )

            logger.debug("Sending chat request to Ollama: model={}, messages={}", config.model, messages.size)

            val response = webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaChatResponse::class.java)
                .timeout(config.timeout)
                .onErrorMap(java.util.concurrent.TimeoutException::class.java) {
                    LLMTimeoutException("Ollama request timed out after ${config.timeout.seconds}s")
                }
                .onErrorMap(WebClientResponseException::class.java) { ex ->
                    LLMServiceUnavailableException(
                        "Ollama service error: ${ex.statusCode} - ${ex.responseBodyAsString}",
                        ex
                    )
                }
                .block() ?: throw LLMServiceUnavailableException("No response from Ollama")

            val durationMs = System.currentTimeMillis() - startTime
            logger.info("Ollama chat completed: model={}, duration={}ms", config.model, durationMs)

            return response.message.content
        } catch (ex: Exception) {
            when (ex) {
                is LLMServiceUnavailableException,
                is LLMTimeoutException -> throw ex
                else -> throw LLMServiceUnavailableException("Failed to communicate with Ollama", ex)
            }
        }
    }

    override fun chatWithJson(messages: List<ChatMessage>, jsonSchema: String?): String {
        // Add JSON mode instruction to the system message
        val enhancedMessages = messages.toMutableList()

        if (enhancedMessages.isNotEmpty() && enhancedMessages[0].role == ChatMessage.Role.SYSTEM) {
            val systemContent = enhancedMessages[0].content + "\n\nYou must respond with valid JSON only. No markdown, no explanations."
            enhancedMessages[0] = ChatMessage(ChatMessage.Role.SYSTEM, systemContent)
        } else {
            enhancedMessages.add(0, ChatMessage(
                ChatMessage.Role.SYSTEM,
                "You must respond with valid JSON only. No markdown, no explanations."
            ))
        }

        jsonSchema?.let {
            enhancedMessages.add(0, ChatMessage(
                ChatMessage.Role.SYSTEM,
                "Expected JSON schema:\n$it"
            ))
        }

        val response = chat(enhancedMessages, temperature = 0.3) // Lower temperature for structured output

        // Try to extract JSON from markdown code blocks if present
        return extractJson(response)
    }

    override fun getProviderName(): String = "ollama"

    override fun getModelName(): String = config.model

    override fun isHealthy(): Boolean {
        return try {
            webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(5))
                .map { true }
                .onErrorReturn(false)
                .block() ?: false
        } catch (ex: Exception) {
            logger.warn("Ollama health check failed", ex)
            false
        }
    }

    /**
     * Extracts JSON from response, handling markdown code blocks.
     */
    private fun extractJson(response: String): String {
        // Try to find JSON in markdown code blocks
        val jsonBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val match = jsonBlockRegex.find(response)

        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            // Try to find raw JSON (starts with { or [)
            val trimmed = response.trim()
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                trimmed
            } else {
                throw LLMResponseParsingException("Could not extract JSON from response: $response")
            }
        }
    }

    private fun ChatMessage.toOllamaMessage() = OllamaMessage(
        role = when (role) {
            ChatMessage.Role.SYSTEM -> "system"
            ChatMessage.Role.USER -> "user"
            ChatMessage.Role.ASSISTANT -> "assistant"
        },
        content = content
    )
}

// Ollama API DTOs
private data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions? = null
)

private data class OllamaMessage(
    val role: String,
    val content: String
)

private data class OllamaOptions(
    val temperature: Double
)

private data class OllamaChatResponse(
    val model: String,
    val message: OllamaMessage,
    @JsonProperty("created_at")
    val createdAt: String,
    val done: Boolean
)
