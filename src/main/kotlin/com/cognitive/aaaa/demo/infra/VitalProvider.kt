package com.cognitive.aaaa.demo.infra

import com.cognitive.aaaa.demo.domain.repository.UserRepository
import com.vital.api.Vital
import com.vital.api.resources.link.requests.LinkTokenExchange
import com.vital.api.resources.user.requests.UserCreateBody
import org.springframework.stereotype.Component

@Component
class VitalProvider(
    val vital: Vital,
    val userRepository: UserRepository,
) {

    fun link(
        userId: String,
        redirectUrl: String,
    ): String {
        try {
            println("VitalProvider: Creating link for userId: $userId, redirectUrl: $redirectUrl")
            val request = LinkTokenExchange.builder()
                .userId(userId)
                .redirectUrl(redirectUrl)
                .build()
            println("VitalProvider: LinkTokenExchange request built")
            val data = vital.link().token(request)
            println("VitalProvider: Link token response received: ${data.linkWebUrl}")
            return data.linkWebUrl
        } catch (e: Exception) {
            println("VitalProvider: Error creating link: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun createVitalUser(userId: String): String? {
        try {
            println("VitalProvider: Creating Vital user for userId: $userId")
            val user = userRepository.findById(userId).orElse(null)
            if (user != null) {
                // If user already has a valid Vital user ID, return it
                if (user.junctionUserId != null && isValidVitalUserId(user.junctionUserId)) {
                    println("VitalProvider: User already has valid Vital user ID: ${user.junctionUserId}")
                    return user.junctionUserId
                }
                
                // If user has an invalid junctionUserId, clear it
                if (user.junctionUserId != null && !isValidVitalUserId(user.junctionUserId)) {
                    println("VitalProvider: User has invalid Vital user ID: ${user.junctionUserId}, clearing it...")
                    userRepository.save(user.copy(junctionUserId = null))
                }
                
                println("VitalProvider: Creating new Vital user with client_user_id: $userId")
                val request = UserCreateBody.builder()
                    .clientUserId(userId)
                    .build()

                val response = vital.user().create(request)
                val vitalUserId = response.userId
                println("VitalProvider: Created Vital user with ID: $vitalUserId")
                
                // Update user with Vital user ID
                userRepository.save(user.copy(junctionUserId = vitalUserId))
                println("VitalProvider: Updated user with Vital user ID")
                return vitalUserId
            } else {
                println("VitalProvider: User not found in database: $userId")
            }
        } catch (e: Exception) {
            println("VitalProvider: Error creating Vital user: ${e.message}")
            e.printStackTrace()
            throw e
        }
        return null
    }
    
    private fun isValidVitalUserId(userId: String?): Boolean {
        if (userId == null) return false
        // Vital user IDs should be UUIDs (36 characters with hyphens)
        return userId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
    }
    
    // Simplified method - for now return empty list until we figure out the correct API
    fun getUserConnectedProviders(vitalUserId: String): List<Map<String, Any>> {
        try {
            // TODO: Implement proper provider retrieval once we understand the correct API
            println("Getting providers for user: $vitalUserId")
            return emptyList()
        } catch (e: Exception) {
            println("Error fetching user providers: ${e.message}")
            return emptyList()
        }
    }
}
