package com.cognitive.aaaa.demo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(name = "supabase_user_id", nullable = false)
    val supabaseUserId: String = "",
    
    @Column(unique = true, nullable = false)
    val email: String = "",
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val totalTestsCompleted: Int = 0,
    
    val lastLogin: LocalDateTime? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val updatedAt: LocalDateTime? = null
) {
    constructor() : this("", "", UserRole.USER, true, 0, null, LocalDateTime.now(), null)
}

enum class UserRole {
    USER, ADMIN, SUPER_ADMIN
}
