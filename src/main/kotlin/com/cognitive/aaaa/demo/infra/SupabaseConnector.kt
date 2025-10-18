package com.cognitive.aaaa.demo.infra

import com.cognitive.aaaa.demo.domain.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SupabaseConnector(
    private val userRepository: UserRepository
) {

    @Value("supabase.url")
    private lateinit var supabaseUrl: String

    @Value("supabase.anon-key")
    private lateinit var supabaseAnonKey: String



}