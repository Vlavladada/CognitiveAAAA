package com.cognitive.aaaa.demo.service

import com.cognitive.aaaa.demo.model.User
import com.cognitive.aaaa.demo.model.UserRole
import com.cognitive.aaaa.demo.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    
    fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            userRepository.findByEmail(authentication.name).orElse(null)
        } else null
    }
    
    fun getCurrentUserId(): String? {
        return getCurrentUser()?.supabaseUserId
    }
    
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser"
    }
    
    fun registerUser(email: String, password: String): User {
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        
        val encodedPassword = passwordEncoder.encode(password)
        val user = User(
            supabaseUserId = "user-${UUID.randomUUID().toString().substring(0, 8)}",
            email = email,
            role = UserRole.USER,
            isActive = true,
            totalTestsCompleted = 0,
            createdAt = LocalDateTime.now()
        )
        
        return userRepository.save(user)
    }
    
    fun updateUserLogin(userId: String): User? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        val updatedUser = user.copy(lastLogin = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        return userRepository.save(updatedUser)
    }
    
    fun updateUserTestCount(user: User): User {
        val updatedUser = user.copy(
            totalTestsCompleted = user.totalTestsCompleted + 1,
            updatedAt = LocalDateTime.now()
        )
        return userRepository.save(updatedUser)
    }
    
    fun findUserByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }
}