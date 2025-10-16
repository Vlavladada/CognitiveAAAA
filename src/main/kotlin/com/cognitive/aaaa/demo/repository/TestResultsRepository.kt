package com.cognitive.aaaa.demo.repository

import com.cognitive.aaaa.demo.model.TestResults
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TestResultsRepository : JpaRepository<TestResults, Long> {
    fun findBySessionId(sessionId: Long): Optional<TestResults>
}
