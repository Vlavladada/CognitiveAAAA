package com.cognitive.aaaa.demo.repository

import com.cognitive.aaaa.demo.model.Trial
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TrialRepository : JpaRepository<Trial, Long> {
    fun findBySessionIdOrderByTrialNumber(sessionId: Long): List<Trial>
}
