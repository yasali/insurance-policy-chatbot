package com.insurance.policies.domain.repository

import com.insurance.policies.domain.model.Conversation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ConversationRepository : JpaRepository<Conversation, UUID> {

    /**
     * Finds a conversation with all messages eagerly loaded.
     */
    @Query("""
        SELECT DISTINCT c FROM Conversation c
        LEFT JOIN FETCH c._messages
        WHERE c.id = :id
    """)
    fun findByIdWithMessages(@Param("id") id: UUID): Conversation?
}
