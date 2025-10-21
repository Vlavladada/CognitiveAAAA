package com.cognitive.aaaa.demo.infra.service

import com.cognitive.aaaa.demo.domain.model.User
import com.cognitive.aaaa.demo.domain.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.*

@Service
class JunctionService(
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate
) {
    
    @Value("\${junction.api-key}")
    private lateinit var apiKey: String
    
    @Value("\${junction.environment}")
    private lateinit var environment: String
    
    private val baseUrl = "https://api.sandbox.eu.junction.com"
    
    data class JunctionUser(
        val id: String,
        val clientUserId: String,
        val createdAt: String
    )
    
    data class JunctionUserRequest(
        val clientUserId: String
    )
    
    data class ConnectedDevice(
        val id: String,
        val name: String,
        val provider: String,
        val type: String,
        val status: String,
        val connectedAt: String?
    )
    
    data class DeviceListResponse(
        val devices: List<ConnectedDevice>
    )
    
    /**
     * Create a Junction user for the given application user
     */
    fun createJunctionUser(user: User): String? {
        try {
            val headers = HttpHeaders().apply {
                set("x-vital-api-key", apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            
            val request = JunctionUserRequest(clientUserId = user.id)
            val entity = HttpEntity(request, headers)
            
            val response = restTemplate.exchange(
                "$baseUrl/v2/user",
                HttpMethod.POST,
                entity,
                JunctionUser::class.java
            )
            
            val junctionUser = response.body
            return junctionUser?.id
            
        } catch (e: Exception) {
            println("Error creating Junction user: ${e.message}")
            return null
        }
    }
    
    /**
     * Get connected devices for a Junction user
     */
    fun getConnectedDevices(junctionUserId: String): List<ConnectedDevice> {
        try {
            val headers = HttpHeaders().apply {
                set("x-vital-api-key", apiKey)
            }
            
            val entity = HttpEntity<String>(headers)
            
            val response = restTemplate.exchange(
                "$baseUrl/v2/devices?user_id=$junctionUserId",
                HttpMethod.GET,
                entity,
                DeviceListResponse::class.java
            )
            
            return response.body?.devices ?: emptyList()
            
        } catch (e: Exception) {
            println("Error fetching connected devices: ${e.message}")
            return emptyList()
        }
    }
    
    /**
     * Update user's connected devices
     */
    fun updateUserDevices(user: User): User {
        val junctionUserId = user.junctionUserId ?: return user
        
        val devices = getConnectedDevices(junctionUserId)
        val devicesJson = objectMapper.writeValueAsString(devices)
        
        val updatedUser = user.copy(
            connectedDevices = devicesJson,
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    /**
     * Get user's connected devices as a list
     */
    fun getUserConnectedDevices(user: User): List<ConnectedDevice> {
        return if (user.connectedDevices != null) {
            try {
                objectMapper.readValue(user.connectedDevices, Array<ConnectedDevice>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Generate Junction Link URL for device connection
     */
    fun generateLinkUrl(user: User): String {
        try {
            val junctionUserId = user.junctionUserId ?: createJunctionUser(user) ?: return ""
            
            // Update user with Junction ID if not already set
            if (user.junctionUserId == null) {
                val updatedUser = user.copy(
                    junctionUserId = junctionUserId,
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(updatedUser)
            }
            
            // Generate Junction Link using the correct API endpoint
            val headers = HttpHeaders().apply {
                set("X-Vital-API-Key", apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            
            val linkRequest = mapOf(
                "user_id" to junctionUserId,
                "redirect_url" to "http://localhost:8080/devices" // Redirect after connection
            )
            
            val entity = HttpEntity(linkRequest, headers)
            
            val response = restTemplate.exchange(
                "$baseUrl/v2/link",
                HttpMethod.POST,
                entity,
                Map::class.java
            )
            
            val responseBody = response.body as? Map<String, Any>
            val linkUrl = responseBody?.get("link_url") as? String
            
            return linkUrl ?: ""
            
        } catch (e: Exception) {
            println("Error generating Junction link: ${e.message}")
            return ""
        }
    }
}
