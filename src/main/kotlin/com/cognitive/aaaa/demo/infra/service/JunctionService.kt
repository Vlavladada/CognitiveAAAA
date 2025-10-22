package com.cognitive.aaaa.demo.infra.service

import com.cognitive.aaaa.demo.domain.model.User
import com.cognitive.aaaa.demo.domain.repository.UserRepository
import com.cognitive.aaaa.demo.infra.VitalProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class JunctionService(
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val vitalProvider: VitalProvider
) {
    
    data class ConnectedDevice(
        val name: String,
        val slug: String,
        val logo: String,
        val status: String,
        val connectedAt: String?,
        val resourceAvailability: Map<String, Any>?
    )

    /**
     * Create a Vital user for the given user
     */
    fun createJunctionUser(user: User): String? {
        return vitalProvider.createVitalUser(user.id.toString())
    }
    
    /**
     * Get connected devices for a Vital user
     */
    fun getConnectedDevices(vitalUserId: String): List<ConnectedDevice> {
        val providers = vitalProvider.getUserConnectedProviders(vitalUserId)
        return providers.map { provider ->
            ConnectedDevice(
                name = provider["name"] as? String ?: "",
                slug = provider["slug"] as? String ?: "",
                logo = provider["logo"] as? String ?: "",
                status = provider["status"] as? String ?: "",
                connectedAt = provider["created_on"] as? String,
                resourceAvailability = provider["resource_availability"] as? Map<String, Any>
            )
        }
    }
    
    /**
     * Update user's connected devices
     */
    fun updateUserDevices(user: User): User {
        val vitalUserId = user.junctionUserId ?: return user
        
        val devices = getConnectedDevices(vitalUserId)
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
     * Generate link URL for user to connect devices
     */
    fun generateLinkUrl(user: User): String {
        try {
            val vitalUserId = user.junctionUserId ?: createJunctionUser(user) ?: return ""
            
            // Update user with Vital ID if not already set
            if (user.junctionUserId == null) {
                val updatedUser = user.copy(
                    junctionUserId = vitalUserId,
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(updatedUser)
            }

            return vitalProvider.link(vitalUserId, "https://your-app.com/callback")
        } catch (e: Exception) {
            println("Error generating Vital link: ${e.message}")
            e.printStackTrace()
            return ""
        }
    }
}