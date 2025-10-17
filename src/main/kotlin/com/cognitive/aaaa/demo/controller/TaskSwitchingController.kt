package com.cognitive.aaaa.demo.controller

import com.cognitive.aaaa.demo.model.*
import com.cognitive.aaaa.demo.service.TaskSwitchingService
import com.cognitive.aaaa.demo.service.AuthenticationService
import com.cognitive.aaaa.demo.service.SupabaseAuthService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class TaskSwitchingController(
    private val taskSwitchingService: TaskSwitchingService,
    private val authenticationService: AuthenticationService,
    private val supabaseAuthService: SupabaseAuthService
) {
    
    @GetMapping("/")
    fun index(model: Model): String {
        val user = authenticationService.getCurrentUser()
        model.addAttribute("isAuthenticated", authenticationService.isAuthenticated())
        model.addAttribute("user", user)
        return "greeting"
    }
    
    @PostMapping("/api/session")
    @ResponseBody
    fun createSession(
        @RequestParam(required = false) participantId: String?,
        @RequestHeader(value = "X-User-ID", required = false) userId: String?
    ): ResponseEntity<TestSession> {
        val user = if (userId != null) {
            supabaseAuthService.getUserBySupabaseId(userId)
        } else {
            authenticationService.getCurrentUser()
        }
        val session = taskSwitchingService.createSession(user, participantId)
        return ResponseEntity.ok(session)
    }
    
    @PostMapping("/api/session/{sessionId}/training")
    @ResponseBody
    fun startTraining(@PathVariable sessionId: String): ResponseEntity<List<Trial>> {
        val trials = taskSwitchingService.startTraining(sessionId)
        return ResponseEntity.ok(trials)
    }
    
    @PostMapping("/api/session/{sessionId}/test")
    @ResponseBody
    fun startTest(@PathVariable sessionId: String): ResponseEntity<List<Trial>> {
        val trials = taskSwitchingService.startTest(sessionId)
        return ResponseEntity.ok(trials)
    }
    
    @GetMapping("/api/session/{sessionId}/trial")
    @ResponseBody
    fun getCurrentTrial(@PathVariable sessionId: String): ResponseEntity<Trial?> {
        val trial = taskSwitchingService.getCurrentTrial(sessionId)
        return ResponseEntity.ok(trial)
    }
    
    @PostMapping("/api/session/{sessionId}/response")
    @ResponseBody
    fun recordResponse(
        @PathVariable sessionId: String,
        @RequestBody responseRequest: ResponseRequest
    ): ResponseEntity<Trial?> {
        val trial = taskSwitchingService.recordResponse(
            sessionId, 
            responseRequest.response, 
            responseRequest.responseTime
        )
        return ResponseEntity.ok(trial)
    }
    
    @PostMapping("/api/session/{sessionId}/complete")
    @ResponseBody
    fun completeSession(@PathVariable sessionId: String): ResponseEntity<TestResults> {
        val results = taskSwitchingService.completeSession(sessionId)
        return ResponseEntity.ok(results)
    }
    
    @GetMapping("/api/session/{sessionId}")
    @ResponseBody
    fun getSession(@PathVariable sessionId: String): ResponseEntity<TestSession?> {
        val session = taskSwitchingService.getSession(sessionId)
        return ResponseEntity.ok(session)
    }

    @GetMapping("/api/user/sessions")
    @ResponseBody
    fun getUserSessions(): ResponseEntity<List<TestSession>> {
        val user = authenticationService.getCurrentUser()
        return if (user != null) {
            val sessions = taskSwitchingService.getUserSessions(user.supabaseUserId)
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