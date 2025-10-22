package com.cognitive.aaaa.demo.application.controller

import com.cognitive.aaaa.demo.application.service.AuthenticationService
import com.cognitive.aaaa.demo.domain.repository.UserRepository
import com.cognitive.aaaa.demo.infra.VitalProvider
import com.cognitive.aaaa.demo.infra.service.JunctionService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class JunctionController(
    private val junctionService: JunctionService,
    private val userRepository: UserRepository,
    private val vitalProvider: VitalProvider,
    private val authenticationService: AuthenticationService,
) {
    
    @PostMapping("/api/junction/link")
    @ResponseBody
    fun addIntegration(
        @RequestBody linkRequest: LinkRequest,
    ): ResponseEntity<Map<String, String>> {
        try {
            println("JunctionController: Received link request for userId: ${linkRequest.userId}")
            
            // Check if user exists in our database
            if (!userRepository.existsById(linkRequest.userId)) {
                println("JunctionController: User not found in database: ${linkRequest.userId}")
                return ResponseEntity.badRequest().body(mapOf("error" to "User not found"))
            }
            
            println("JunctionController: User found, creating/getting Vital user...")
            
            // Create Vital user if not exists and get Vital user ID
            val vitalUserId = vitalProvider.createVitalUser(linkRequest.userId)
            if (vitalUserId == null) {
                println("JunctionController: Failed to create/get Vital user")
                return ResponseEntity.badRequest().body(mapOf("error" to "Failed to create Vital user"))
            }
            
            println("JunctionController: Vital user ID: $vitalUserId")
            println("JunctionController: Generating link with redirectUrl: ${linkRequest.redirectUrl}")
            
            // Generate link URL using Vital user ID
            val linkUrl = vitalProvider.link(vitalUserId, linkRequest.redirectUrl)
            println("JunctionController: Generated link URL: $linkUrl")
            
            return ResponseEntity.ok(mapOf("linkUrl" to linkUrl))
        } catch (e: Exception) {
            println("JunctionController: Error occurred: ${e.message}")
            e.printStackTrace()
            return ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    data class LinkRequest(
        val userId: String,
        val redirectUrl: String,
    )
    
    @GetMapping("/api/junction/devices")
    @ResponseBody
    fun getConnectedDevices(): ResponseEntity<List<JunctionService.ConnectedDevice>> {
        val user = authenticationService.getCurrentUser()
        if (user == null) {
            return ResponseEntity.badRequest().build()
        }
        
        val devices = junctionService.getUserConnectedDevices(user)
        return ResponseEntity.ok(devices)
    }
    
    @PostMapping("/api/junction/refresh")
    @ResponseBody
    fun refreshDevices(): ResponseEntity<Map<String, Any>> {
        val user = authenticationService.getCurrentUser()
        if (user == null) {
            return ResponseEntity.badRequest().body(mapOf("error" to "User not authenticated"))
        }
        
        val updatedUser = junctionService.updateUserDevices(user)
        val devices = junctionService.getUserConnectedDevices(updatedUser)
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "devices" to devices,
            "deviceCount" to devices.size
        ))
    }
    
    @GetMapping("/junction-test")
    fun junctionTestPage(): String {
        return "junction-test"
    }
    
    @GetMapping("/junction-callback")
    fun junctionCallbackPage(): String {
        return "junction-callback"
    }
}
