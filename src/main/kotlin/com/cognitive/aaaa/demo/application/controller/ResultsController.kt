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

        val results = testResultsRepository.findBySessionId(session.id).orElse(null)
        if (results == null) {
            return "redirect:/?error=results_not_found"
        }

        model.addAttribute("session", session)
        model.addAttribute("results", results)
        model.addAttribute("isAuthenticated", authenticationService.isAuthenticated())
        model.addAttribute("currentUser", authenticationService.getCurrentUser())

        return "results"
    }

    @GetMapping("/api/results/{sessionId}")
    @ResponseBody
    fun getResults(@PathVariable sessionId: UUID): ResponseEntity<TestResults?> {
        val session = testSessionRepository.findById(sessionId).orElse(null)
        if (session == null) {
            return ResponseEntity.notFound().build()
        }

        val results = testResultsRepository.findBySessionId(session.id).orElse(null)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/api/user/results")
    @ResponseBody
    fun getUserResults(): ResponseEntity<List<TestResults>> {
        val user = authenticationService.getCurrentUser()
        if (user == null) {
            return ResponseEntity.ok(emptyList())
        }

        val sessions = testSessionRepository.findByUserIdAndIsCompletedTrueOrderByStartTimeDesc(user.id)
        val results = sessions.mapNotNull { session ->
            testResultsRepository.findBySessionId(session.id).orElse(null)
        }

        return ResponseEntity.ok(results)
    }
}