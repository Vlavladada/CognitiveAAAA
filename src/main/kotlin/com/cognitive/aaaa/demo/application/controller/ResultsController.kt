package com.cognitive.aaaa.demo.application.controller

import com.cognitive.aaaa.demo.domain.model.TestResults
import com.cognitive.aaaa.demo.domain.repository.TestResultsRepository
import com.cognitive.aaaa.demo.domain.repository.TestSessionRepository
import com.cognitive.aaaa.demo.application.service.AuthenticationService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.util.UUID

@Controller
class ResultsController(
    private val testResultsRepository: TestResultsRepository,
    private val testSessionRepository: TestSessionRepository,
    private val authenticationService: AuthenticationService
) {

    @GetMapping("/results/{sessionId}")
    fun showResults(@PathVariable sessionId: UUID, model: Model): String {
        val session = testSessionRepository.findById(sessionId).orElse(null)
        if (session == null) {
            return "redirect:/?error=session_not_found"
        }

        val results = testResultsRepository.findBySessionId(session.id!!).orElse(null)
        if (results == null) {
            return "redirect:/?error=results_not_found"
        }

        model.addAttribute("session", session)
        model.addAttribute("results", results)
        // For web page requests, we can't get user from headers
        // The frontend will handle authentication via JavaScript
        model.addAttribute("isAuthenticated", false)
        model.addAttribute("currentUser", null)

        return "results"
    }

    @GetMapping("/api/results/{sessionId}")
    @ResponseBody
    fun getResults(@PathVariable sessionId: UUID): ResponseEntity<TestResults?> {
        val session = testSessionRepository.findById(sessionId).orElse(null)
        if (session == null) {
            return ResponseEntity.notFound().build()
        }

        val results = testResultsRepository.findBySessionId(session.id!!).orElse(null)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/api/user/results")
    @ResponseBody
    fun getUserResults(): ResponseEntity<List<Map<String, Any?>>> {
        val user = authenticationService.getCurrentUser()
        if (user == null) {
            return ResponseEntity.ok(emptyList())
        }

        val sessions = testSessionRepository.findByUserIdAndIsCompletedTrueOrderByStartTimeDesc(user.id)
        val results = sessions.mapNotNull { session ->
            testResultsRepository.findBySessionId(session.id!!).orElse(null)?.let { result ->
                mapOf(
                    "id" to result.id,
                    "session" to mapOf("sessionId" to session.id),
                    "totalTrials" to result.totalTrials,
                    "correctTrials" to result.correctTrials,
                    "accuracy" to result.accuracy,
                    "averageResponseTime" to result.averageResponseTime,
                    "switchCost" to result.switchCost,
                    "taskInterference" to result.taskInterference,
                    "errorCount" to result.errorCount,
                    "colorTaskAccuracy" to result.colorTaskAccuracy,
                    "shapeTaskAccuracy" to result.shapeTaskAccuracy,
                    "colorTaskAvgRt" to result.colorTaskAvgRt,
                    "shapeTaskAvgRt" to result.shapeTaskAvgRt,
                    "congruentAvgRt" to result.congruentAvgRt,
                    "incongruentAvgRt" to result.incongruentAvgRt,
                    "switchAvgRt" to result.switchAvgRt,
                    "repeatAvgRt" to result.repeatAvgRt,
                    "createdAt" to result.createdAt
                )
            }
        }

        return ResponseEntity.ok(results)
    }
}