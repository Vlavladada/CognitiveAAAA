package com.cognitive.aaaa.demo.repository

import com.cognitive.aaaa.demo.model.TestSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TestSessionRepository : JpaRepository<TestSession, Long> {
    fun findBySessionId(sessionId: String): Optional<TestSession>
    fun findByUserSupabaseUserIdOrderByStartTimeDesc(supabaseUserId: String): List<TestSession>
    fun findByUserSupabaseUserIdAndIsCompletedTrueOrderByStartTimeDesc(supabaseUserId: String): List<TestSession>
}
