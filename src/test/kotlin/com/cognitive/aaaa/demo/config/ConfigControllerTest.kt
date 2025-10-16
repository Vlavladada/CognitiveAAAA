package com.cognitive.aaaa.demo.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "supabase.url=https://test-project.supabase.co",
    "supabase.anon-key=test-anon-key",
    "app.version=1.0.0",
    "app.environment=test"
])
class ConfigControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return public configuration`() {
        mockMvc.perform(get("/api/config/public"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.supabaseUrl").value("https://test-project.supabase.co"))
            .andExpect(jsonPath("$.supabaseAnonKey").value("test-anon-key"))
            .andExpect(jsonPath("$.appVersion").value("1.0.0"))
            .andExpect(jsonPath("$.environment").value("test"))
    }
}
