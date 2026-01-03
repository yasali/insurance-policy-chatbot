package com.insurance.policies.presentation.controller

import com.insurance.policies.application.dto.ChatRequest
import com.insurance.policies.application.dto.ChatResponse
import com.insurance.policies.application.dto.ConversationResponse
import com.insurance.policies.application.service.PolicyAssistantService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/assistant")
class AssistantController(
    private val assistantService: PolicyAssistantService
) {

    /**
     * Sends a message to the LLM.
     *
     * POST /api/v1/assistant/chat
     */
    @PostMapping("/chat")
    fun chat(@Valid @RequestBody request: ChatRequest): ResponseEntity<ChatResponse> {
        val response = assistantService.chat(request)
        return ResponseEntity.ok(response)
    }

    /**
     * Gets a conversation by ID with all messages.
     *
     * GET /api/v1/assistant/conversations/{id}
     */
    @GetMapping("/conversations/{id}")
    fun getConversation(@PathVariable id: UUID): ResponseEntity<ConversationResponse> {
        val response = assistantService.getConversation(id)
        return ResponseEntity.ok(response)
    }
}
