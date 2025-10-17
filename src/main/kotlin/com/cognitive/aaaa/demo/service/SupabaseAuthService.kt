package com.cognitive.aaaa.demo.service

import com.cognitive.aaaa.demo.model.User
import com.cognitive.aaaa.demo.model.UserRole
import com.cognitive.aaaa.demo.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class SupabaseAuthService(
    private val userRepository: UserRepository
) {
    
    @Value("\${supabase.url:https://your-project.supabase.co}")
    private lateinit var supabaseUrl: String
    
    @Value("\${supabase.anon-key:your-anon-key-here}")
    private lateinit var supabaseAnonKey: String
    
    fun createUserFromSupabase(supabaseUserId: String, email: String): User {
        val existingUser = userRepository.findBySupabaseUserId(supabaseUserId).orElse(null)
        
        return if (existingUser != null) {
            val updatedUser = existingUser.copy(
                email = email,
                lastLogin = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            userRepository.save(updatedUser)
        } else {
            val newUser = User(
                supabaseUserId = supabaseUserId,
                email = email,
                role = UserRole.USER,
                isActive = true,
                totalTestsCompleted = 0,
                lastLogin = LocalDateTime.now(),
                createdAt = LocalDateTime.now()
            )
            userRepository.save(newUser)
        }
    }
    
    fun getUserBySupabaseId(supabaseUserId: String): User? {
        return userRepository.findBySupabaseUserId(supabaseUserId).orElse(null)
    }
    
    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }
    
    fun saveUser(user: User): User {
        return userRepository.save(user)
    }
    
    fun updateUserLastLogin(user: User): User {
        return userRepository.save(user.copy(lastLogin = LocalDateTime.now()))
    }
    
    fun generateMockSupabaseUserId(): String {
        return "supabase-user-${UUID.randomUUID().toString().substring(0, 8)}"
    }
}
