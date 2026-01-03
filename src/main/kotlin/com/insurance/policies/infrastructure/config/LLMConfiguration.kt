package com.insurance.policies.infrastructure.config

import com.insurance.policies.infrastructure.llm.LLMProvider
import com.insurance.policies.infrastructure.llm.OllamaProvider
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration for LLM providers.
 * This allows easy switching between different LLM providers.
 */
@Configuration
class LLMConfiguration {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun llmProvider(
        config: LLMConfig,
        webClientBuilder: WebClient.Builder
    ): LLMProvider {
        logger.info("Initializing LLM provider: {} with model: {}", config.provider, config.model)

        return when (config.provider.lowercase()) {
            "ollama" -> OllamaProvider(config, webClientBuilder)
            // Add more providers here as needed: [openai, gemini, groq, etc.]
            else -> {
                logger.warn("Unknown provider '{}', falling back to Ollama", config.provider)
                OllamaProvider(config, webClientBuilder)
            }
        }
    }
}
