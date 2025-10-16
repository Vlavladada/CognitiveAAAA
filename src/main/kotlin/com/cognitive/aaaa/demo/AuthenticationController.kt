package com.cognitive.aaaa.demo

import com.cognitive.aaaa.demo.service.AuthenticationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {
    
    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<Map<String, Any?>> {
        val user = authenticationService.getCurrentUser()
        return if (user != null) {
            ResponseEntity.ok(mapOf(
                "id" to user.id,
                "email" to user.email,
                "totalTestsCompleted" to user.totalTestsCompleted,
                "lastLogin" to user.lastLogin
            ))
        } else {
            ResponseEntity.ok(mapOf("authenticated" to false))
        }
    }
    
    @GetMapping("/status")
    fun getAuthStatus(): ResponseEntity<Map<String, Boolean>> {
        return ResponseEntity.ok(mapOf("authenticated" to authenticationService.isAuthenticated()))
    }
}
