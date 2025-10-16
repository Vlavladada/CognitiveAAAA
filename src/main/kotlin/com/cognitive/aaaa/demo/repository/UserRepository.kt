package com.cognitive.aaaa.demo.repository

import com.cognitive.aaaa.demo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByEmail(email: String): Optional<User>
    fun findBySupabaseUserId(supabaseUserId: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun existsBySupabaseUserId(supabaseUserId: String): Boolean
}
