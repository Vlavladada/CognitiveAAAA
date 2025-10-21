package com.cognitive.aaaa.demo.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(nullable = false)
    val id: String,
    
    @Column(unique = true, nullable = false)
    val email: String = "",
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val totalTestsCompleted: Int = 0,
    
    val lastLogin: LocalDateTime? = LocalDateTime.now(),
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val updatedAt: LocalDateTime? = LocalDateTime.now()
) {
    constructor() : this("", "", UserRole.USER, true, 0, null, LocalDateTime.now(), null)
}

enum class UserRole {
    USER
}
