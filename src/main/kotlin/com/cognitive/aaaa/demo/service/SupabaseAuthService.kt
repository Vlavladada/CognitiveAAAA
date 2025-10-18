package com.cognitive.aaaa.demo.service

import com.cognitive.aaaa.demo.domain.model.User
import com.cognitive.aaaa.demo.domain.model.UserRole
import com.cognitive.aaaa.demo.domain.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SupabaseAuthService(
    private val userRepository: UserRepository
) {
    
    @Value("\${supabase.url:https://your-project.supabase.co}")
    private lateinit var supabaseUrl: String
    
    @Value("\${supabase.anon-key:your-anon-key-here}")
    private lateinit var supabaseAnonKey: String
    
    fun createUserFromSupabase(supabaseUserId: String, email: String): User {
        val existingUser = userRepository.findById(supabaseUserId).orElse(null)
        
        return if (existingUser != null) {
            val updatedUser = existingUser.copy(
                email = email,
                lastLogin = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            userRepository.save(updatedUser)
        } else {
            val newUser = User(
                id = supabaseUserId,
                email = email,
                role = UserRole.USER,
                isActive = true,
                totalTestsCompleted = 0,
                lastLogin = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                createdAt = LocalDateTime.now()
            )
            userRepository.save(newUser)
        }
    }
    
    fun getUserBySupabaseId(supabaseUserId: String): User? {
        return userRepository.findById(supabaseUserId).orElse(null)
    }
    
    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }
}
