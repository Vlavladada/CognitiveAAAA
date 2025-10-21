package com.cognitive.aaaa.demo.domain.repository

import com.cognitive.aaaa.demo.domain.model.TestResults
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface TestResultsRepository : JpaRepository<TestResults, Long> {
    fun findBySessionId(sessionId: UUID): Optional<TestResults>
}