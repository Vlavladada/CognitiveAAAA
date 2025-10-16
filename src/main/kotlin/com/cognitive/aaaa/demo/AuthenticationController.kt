package com.cognitive.aaaa.demo

import com.cognitive.aaaa.demo.service.SupabaseAuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SupabaseAuthRequest(
    val accessToken: String,
    val refreshToken: String,
    val email: String? = null,
    val supabaseUserId: String? = null
)
data class AuthResponse(val success: Boolean, val message: String, val user: UserResponse? = null)
data class UserResponse(val supabaseUserId: String, val email: String, val role: String)

@RestController
@RequestMapping("/api/auth")
class AuthenticationController(private val supabaseAuthService: SupabaseAuthService) {

    @PostMapping("/supabase")
    fun authenticateWithSupabase(@RequestBody request: SupabaseAuthRequest): ResponseEntity<AuthResponse> {
        return try {
            // In a real implementation, you would verify the Supabase token here
            // For now, we'll extract user info from the token or use the provided data
            val email = request.email ?: "user@example.com" // This should come from token verification
            val supabaseUserId = request.supabaseUserId ?: supabaseAuthService.generateMockSupabaseUserId()
            
            // Check if user already exists by email
            val existingUser = supabaseAuthService.getUserByEmail(email)
            val finalSupabaseUserId = if (existingUser != null) {
                existingUser.supabaseUserId
            } else {
                supabaseUserId
            }
            
            val user = supabaseAuthService.createUserFromSupabase(finalSupabaseUserId, email)
            
            ResponseEntity.ok(AuthResponse(
                success = true,
                message = "Authentication successful",
                user = UserResponse(user.supabaseUserId, user.email, user.role.name)
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(AuthResponse(
                success = false,
                message = "Authentication failed: ${e.message}"
            ))
        }
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(AuthResponse(
            success = true,
            message = "Logged out successfully"
        ))
    }

    @GetMapping("/me")
    fun getCurrentUser(@RequestHeader("X-User-ID") userId: String?): ResponseEntity<UserResponse?> {
        return try {
            if (userId != null) {
                val user = supabaseAuthService.getUserBySupabaseId(userId)
                if (user != null) {
                    ResponseEntity.ok(UserResponse(user.supabaseUserId, user.email, user.role.name))
                } else {
                    ResponseEntity.ok(null)
                }
            } else {
                ResponseEntity.ok(null)
            }
        } catch (e: Exception) {
            ResponseEntity.ok(null)
        }
    }
}