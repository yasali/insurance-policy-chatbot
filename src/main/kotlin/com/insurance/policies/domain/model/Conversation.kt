package com.insurance.policies.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "conversation")
class Conversation(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insurance_id", nullable = false)
    val insurance: Insurance,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(
        mappedBy = "conversation",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("createdAt ASC")
    private val _messages: MutableList<ConversationMessage> = mutableListOf()
) {
    val messages: List<ConversationMessage>
        get() = _messages.toList()

    fun addMessage(message: ConversationMessage) {
        message.conversation = this
        _messages.add(message)
        this.updatedAt = Instant.now()
    }

    fun addMessage(role: MessageRole, content: String): ConversationMessage {
        val message = ConversationMessage(
            role = role,
            content = content
        )
        addMessage(message)
        return message
    }

    /**
     * Gets recent messages with a sliding window approach.
     * Returns first 2 messages + last N messages to keep context manageable.
     */
    fun getRecentMessages(maxMessages: Int = 10): List<ConversationMessage> {
        return when {
            _messages.size <= maxMessages -> _messages.toList()
            else -> _messages.take(2) + _messages.takeLast(maxMessages - 2)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Conversation) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity
@Table(name = "conversation_message")
class ConversationMessage(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    var conversation: Conversation? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: MessageRole,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConversationMessage) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}
