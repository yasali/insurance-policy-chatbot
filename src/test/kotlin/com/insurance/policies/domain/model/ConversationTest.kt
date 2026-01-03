package com.insurance.policies.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Conversation Domain Model")
class ConversationTest {

    private companion object {
        const val VALID_PERSONAL_NUMBER = "199001011234"
    }

    @Nested
    @DisplayName("Message Management")
    inner class MessageManagementTests {

        @Test
        fun `should add message successfully`() {
            // Given
            val conversation = createConversation()
            val message = ConversationMessage(role = MessageRole.USER, content = "Hello")

            // When
            conversation.addMessage(message)

            // Then
            assertEquals(1, conversation.messages.size)
            assertEquals(message, conversation.messages.first())
            assertEquals(conversation, message.conversation)
        }

        @Test
        fun `should add message using convenience method`() {
            // Given
            val conversation = createConversation()
            val content = "What is covered?"

            // When
            val message = conversation.addMessage(MessageRole.USER, content)

            // Then
            assertEquals(1, conversation.messages.size)
            assertEquals(MessageRole.USER, message.role)
            assertEquals(content, message.content)
            assertEquals(conversation, message.conversation)
        }

        @Test
        fun `should add multiple messages in order`() {
            // Given
            val conversation = createConversation()

            // When
            val msg1 = conversation.addMessage(MessageRole.SYSTEM, "System prompt")
            val msg2 = conversation.addMessage(MessageRole.USER, "User question")
            val msg3 = conversation.addMessage(MessageRole.ASSISTANT, "Assistant response")

            // Then
            assertEquals(3, conversation.messages.size)
            assertEquals(msg1, conversation.messages[0])
            assertEquals(msg2, conversation.messages[1])
            assertEquals(msg3, conversation.messages[2])
        }
    }

    @Nested
    @DisplayName("Message Ordering")
    inner class MessageOrderingTests {

        @Test
        fun `should maintain chronological order in sliding window`() {
            // Given
            val conversation = createConversation()
            val messages = mutableListOf<ConversationMessage>()

            // Add messages with increasing content numbers
            repeat(15) { i ->
                messages.add(conversation.addMessage(MessageRole.USER, "Message $i"))
                // Small delay to ensure different timestamps
                Thread.sleep(1)
            }

            // When
            val result = conversation.getRecentMessages(maxMessages = 10)

            // Then
            assertEquals(10, result.size)

            // Verify chronological order
            for (i in 0 until result.size - 1) {
                assertTrue(
                    result[i].createdAt <= result[i + 1].createdAt,
                    "Messages should be in chronological order"
                )
            }
        }

        @Test
        fun `should preserve role sequence in sliding window`() {
            // Given
            val conversation = createConversation()

            conversation.addMessage(MessageRole.SYSTEM, "System")
            conversation.addMessage(MessageRole.USER, "User 1")
            repeat(10) { i ->
                conversation.addMessage(MessageRole.USER, "User $i")
                conversation.addMessage(MessageRole.ASSISTANT, "Assistant $i")
            }

            // When
            val result = conversation.getRecentMessages(maxMessages = 10)

            // Then
            assertEquals(10, result.size)
            assertEquals(MessageRole.SYSTEM, result[0].role, "First should be system")
            assertEquals(MessageRole.USER, result[1].role, "Second should be user")
            // Remaining should alternate based on what's in the last 8
        }
    }

    // Test helpers
    private fun createConversation(): Conversation {
        val insurance = Insurance(personalNumber = VALID_PERSONAL_NUMBER)
        return Conversation(insurance = insurance)
    }

    private fun addMessages(conversation: Conversation, count: Int): List<ConversationMessage> {
        val messages = mutableListOf<ConversationMessage>()
        repeat(count) { i ->
            val role = when (i % 3) {
                0 -> MessageRole.SYSTEM
                1 -> MessageRole.USER
                else -> MessageRole.ASSISTANT
            }
            messages.add(conversation.addMessage(role, "Message $i"))
        }
        return messages
    }
}
