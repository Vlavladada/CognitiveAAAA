package com.cognitive.aaaa.demo.application.service

import com.cognitive.aaaa.demo.domain.model.User
import com.cognitive.aaaa.demo.domain.model.UserRole.USER
import com.cognitive.aaaa.demo.domain.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
) {
    
    fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            userRepository.findByEmail(authentication.name).orElse(null)
        } else null
    }
    
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser"
    }
}