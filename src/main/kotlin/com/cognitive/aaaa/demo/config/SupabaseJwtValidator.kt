package com.cognitive.aaaa.demo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SupabaseJwtValidator(
    @Value("\${supabase.jwt.secret}") private val jwtSecret: String
) {
    
    fun validateToken(token: String): SupabaseUser? {
        return try {
            // For now, we'll do basic validation
            // In production, you should use proper JWT validation
            if (token.isNotEmpty()) {
                SupabaseUser(
                    id = "test-user-id",
                    email = "test@example.com",
                    role = "authenticated",
                    aud = null,
                    exp = null,
                    iat = null,
                    iss = null
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

data class SupabaseUser(
    val id: String,
    val email: String?,
    val role: String,
    val aud: String?,
    val exp: java.util.Date?,
    val iat: java.util.Date?,
    val iss: String?,
    val appMetadata: Map<String, Any>? = null,
    val userMetadata: Map<String, Any>? = null
)
