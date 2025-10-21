package com.cognitive.aaaa.demo.application.controller

import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Response
import com.cognitive.aaaa.demo.domain.model.TestResults
import com.cognitive.aaaa.demo.domain.model.TestSession
import com.cognitive.aaaa.demo.domain.model.Trial
import com.cognitive.aaaa.demo.infra.service.SupabaseAuthService
import com.cognitive.aaaa.demo.application.service.TaskSwitchingService
import com.cognitive.aaaa.demo.application.service.AuthenticationService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Controller
class TaskSwitchingController(
    private val taskSwitchingService: TaskSwitchingService,
    private val authenticationService: AuthenticationService,
    private val supabaseAuthService: SupabaseAuthService
) {
    
    @GetMapping("/")
    fun index(model: Model): String {
        // For web page requests, we can't get user from headers
        // The frontend will handle authentication via JavaScript
        model.addAttribute("isAuthenticated", false)
        model.addAttribute("user", null)
        return "greeting"
    }
    
    @PostMapping("/api/session")
    @ResponseBody
    fun createSession(
        @RequestHeader(value = "X-User-ID", required = false) userId: String?
    ): ResponseEntity<TestSession> {
        val user = if (userId != null) {
            supabaseAuthService.getUserBySupabaseId(userId)
        } else {
            authenticationService.getCurrentUser()
        }
        val session = taskSwitchingService.createSession(user)
        return ResponseEntity.ok(session)
    }
    
    @PostMapping("/api/session/{sessionId}/training")
    @ResponseBody
    fun startTraining(@PathVariable sessionId: String): ResponseEntity<List<Trial>> {
        val trials = taskSwitchingService.startTraining(UUID.fromString(sessionId))
        return ResponseEntity.ok(trials)
    }
    
    @PostMapping("/api/session/{sessionId}/test")
    @ResponseBody
    fun startTest(@PathVariable sessionId: String): ResponseEntity<List<Trial>> {
        val trials = taskSwitchingService.startTest(UUID.fromString(sessionId))
        return ResponseEntity.ok(trials)
    }
    
    @GetMapping("/api/session/{sessionId}/trial")
    @ResponseBody
    fun getCurrentTrial(@PathVariable sessionId: String): ResponseEntity<Trial?> {
        val trial = taskSwitchingService.getCurrentTrial(UUID.fromString(sessionId))
        return ResponseEntity.ok(trial)
    }
    
    @PostMapping("/api/session/{sessionId}/response")
    @ResponseBody
    fun recordResponse(
        @PathVariable sessionId: String,
        @RequestBody responseRequest: ResponseRequest
    ): ResponseEntity<Trial?> {
        val trial = taskSwitchingService.recordResponse(
            UUID.fromString(sessionId),
            responseRequest.response, 
            responseRequest.responseTime
        )
        return ResponseEntity.ok(trial)
    }
    
    @PostMapping("/api/session/{sessionId}/complete")
    @ResponseBody
    fun completeSession(@PathVariable sessionId: String): ResponseEntity<TestResults> {
        val results = taskSwitchingService.completeSession(UUID.fromString(sessionId))
        return ResponseEntity.ok(results)
    }
    
    @GetMapping("/api/session/{sessionId}")
    @ResponseBody
    fun getSession(@PathVariable sessionId: String): ResponseEntity<TestSession?> {
        val session = taskSwitchingService.getSession(UUID.fromString(sessionId))
        return ResponseEntity.ok(session)
    }

    @GetMapping("/api/user/sessions")
    @ResponseBody
    fun getUserSessions(): ResponseEntity<List<TestSession>> {
        val user = authenticationService.getCurrentUser()
        return if (user != null) {
            val sessions = taskSwitchingService.getUserSessions(user.id)
            ResponseEntity.ok(sessions)
        } else {
            ResponseEntity.ok(emptyList())
        }
    }
}

data class ResponseRequest(
    val response: Response?,
    val responseTime: Long
)