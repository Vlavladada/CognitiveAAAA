package com.cognitive.aaaa.demo.application.controller

import com.cognitive.aaaa.demo.domain.repository.UserRepository
import com.cognitive.aaaa.demo.infra.service.JunctionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/junction")
class JunctionCallbackController(
    private val junctionService: JunctionService,
    private val userRepository: UserRepository
) {
    
    @PostMapping("/callback")
    fun handleCallback(@RequestBody callbackData: Map<String, Any>): ResponseEntity<Map<String, String>> {
        try {
            // Handle Vital webhook callback
            // This would typically contain information about successful device connections
            println("Received Vital callback: $callbackData")
            
            // Extract user information from callback if available
            val userId = callbackData["user_id"] as? String
            if (userId != null) {
                // Find user by Vital user ID and update their connected devices
                val user = userRepository.findByJunctionUserId(userId)
                if (user != null) {
                    junctionService.updateUserDevices(user)
                }
            }
            
            return ResponseEntity.ok(mapOf("status" to "success"))
        } catch (e: Exception) {
            println("Error handling Vital callback: ${e.message}")
            return ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}
