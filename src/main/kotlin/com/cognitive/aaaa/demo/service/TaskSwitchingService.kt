package com.cognitive.aaaa.demo.service

import com.cognitive.aaaa.demo.model.*
import org.springframework.stereotype.Service

@Service
class TaskSwitchingService(
    private val supabaseService: SupabaseService
) {
    
    fun createSession(user: User? = null, participantId: String? = null): TestSession {
        return supabaseService.createSession(user?.id, participantId)
    }
    
    fun startTraining(sessionId: String): List<Trial> {
        return supabaseService.startTraining(sessionId)
    }
    
    fun startTest(sessionId: String): List<Trial> {
        return supabaseService.startTest(sessionId)
    }
    
    fun getCurrentTrial(sessionId: String): Trial? {
        return supabaseService.getCurrentTrial(sessionId)
    }
    
    fun recordResponse(sessionId: String, response: Response, responseTime: Long): Trial? {
        return supabaseService.recordResponse(sessionId, response, responseTime)
    }
    
    fun completeSession(sessionId: String): TestResults {
        return supabaseService.completeSession(sessionId)
    }
    
    fun getSession(sessionId: String): TestSession? {
        return supabaseService.getSession(sessionId)
    }
    
    fun getUserSessions(userId: String): List<TestSession> {
        return supabaseService.getUserSessions(userId)
    }
}