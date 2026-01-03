package com.insurance.policies.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "llm")
data class LLMConfig(
    /**
     * LLM provider to use: ollama, openai, gemini, groq
     */
    var provider: String = "ollama",

    /**
     * Model name to use (provider-specific)
     */
    var model: String = "llama3.2",

    /**
     * Base URL for the LLM API
     */
    var baseUrl: String = "http://localhost:11434",

    /**
     * API key (for paid providers)
     */
    var apiKey: String? = null,

    /**
     * Request timeout
     */
    var timeout: Duration = Duration.ofSeconds(60),

    /**
     * Enable response logging for quality tracking
     */
    var enableLogging: Boolean = true,

    /**
     * Temperature for sampling (0.0 = deterministic, 1.0 = creative)
     */
    var temperature: Double = 0.7
)
