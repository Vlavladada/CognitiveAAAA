package com.cognitive.aaaa.demo.service

import com.cognitive.aaaa.demo.config.SupabaseUser
import com.cognitive.aaaa.demo.model.User
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val supabaseService: SupabaseService
) {
    
    fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated) {
            val supabaseUser = authentication.details as? SupabaseUser
            supabaseUser?.let { findOrCreateUser(it) }
        } else null
    }
    
    fun getCurrentUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.name
    }
    
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated
    }
    
    private fun findOrCreateUser(supabaseUser: SupabaseUser): User {
        val existingUser = supabaseService.getUserById(supabaseUser.id)
        return if (existingUser != null) {
            supabaseService.updateUserLogin(supabaseUser.id) ?: existingUser
        } else {
            supabaseService.findOrCreateUser(supabaseUser.id, supabaseUser.email ?: "unknown@example.com")
        }
    }
    
    fun updateUserTestCount(user: User) {
        supabaseService.updateUserTestCount(user)
    }
}