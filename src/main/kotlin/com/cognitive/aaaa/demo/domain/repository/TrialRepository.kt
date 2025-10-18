package com.cognitive.aaaa.demo.domain.repository

import com.cognitive.aaaa.demo.domain.model.Trial
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrialRepository : JpaRepository<Trial, UUID> {
    fun findByIdOrderByTrialNumber(sessionId: UUID): List<Trial>
}