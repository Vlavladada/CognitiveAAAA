package com.cognitive.aaaa.demo.domain.repository

import com.cognitive.aaaa.demo.domain.model.TestSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface TestSessionRepository : JpaRepository<TestSession, UUID> {
    fun findByUserIdOrderByStartTimeDesc(userId: String): List<TestSession>
    fun findByUserIdAndIsCompletedTrueOrderByStartTimeDesc(userId: String): List<TestSession>
}