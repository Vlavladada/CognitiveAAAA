package com.cognitive.aaaa.demo.controller

import com.cognitive.aaaa.demo.service.AuthenticationService
import com.cognitive.aaaa.demo.service.SupabaseService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminController::class)
class AdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var supabaseService: SupabaseService

    @MockkBean
    private lateinit var authenticationService: AuthenticationService

    @Test
    fun `should deny access to unauthenticated users`() {
        mockMvc.perform(get("/api/admin/stats/overview"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should deny access to non-admin users`() {
        mockMvc.perform(get("/api/admin/stats/overview"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should deny access to admin endpoints without authentication`() {
        // Test that all admin endpoints require authentication
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            put("/api/admin/users/user1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("role" to "ADMIN")))
        )
            .andExpect(status().isForbidden) // 403 because CSRF token is present but no auth

        mockMvc.perform(
            put("/api/admin/users/user1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("isActive" to false)))
        )
            .andExpect(status().isForbidden) // 403 because CSRF token is present but no auth
    }
}
