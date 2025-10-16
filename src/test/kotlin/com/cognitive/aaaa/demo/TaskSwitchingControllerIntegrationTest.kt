package com.cognitive.aaaa.demo

import com.cognitive.aaaa.demo.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureWebMvc
class TaskSwitchingControllerIntegrationTest {
    
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    private lateinit var mockMvc: MockMvc
    
    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }
    
    @Test
    fun `should serve index page`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("greeting"))
    }
    
    @Test
    fun `should create session with participant ID`() {
        mockMvc.perform(post("/api/session")
                .param("participantId", "test-participant")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.participantId").value("test-participant"))
            .andExpect(jsonPath("$.currentPhase").value("INSTRUCTIONS"))
            .andExpect(jsonPath("$.currentTrialIndex").value(0))
            .andExpect(jsonPath("$.sessionId").exists())
    }
    
    @Test
    fun `should create session without participant ID`() {
        mockMvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.participantId").isEmpty)
            .andExpect(jsonPath("$.currentPhase").value("INSTRUCTIONS"))
            .andExpect(jsonPath("$.sessionId").exists())
    }
    
    @Test
    fun `should start training phase`() {
        // First create a session
        val sessionResponse = mockMvc.perform(post("/api/session")
                .param("participantId", "test-participant")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andReturn()
        
        val sessionJson = sessionResponse.response.contentAsString
        val session = objectMapper.readValue(sessionJson, TestSession::class.java)
        
        // Start training
        mockMvc.perform(post("/api/session/${session.sessionId}/training"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(10))
    }
    
    @Test
    fun `should start test phase`() {
        // First create a session
        val sessionResponse = mockMvc.perform(post("/api/session")
                .param("participantId", "test-participant")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andReturn()
        
        val sessionJson = sessionResponse.response.contentAsString
        val session = objectMapper.readValue(sessionJson, TestSession::class.java)
        
        // Start test
        mockMvc.perform(post("/api/session/${session.sessionId}/test"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(50))
    }
    
    @Test
    fun `should get current trial`() {
        // First create a session and start training
        val sessionResponse = mockMvc.perform(post("/api/session")
                .param("participantId", "test-participant")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andReturn()
        
        val sessionJson = sessionResponse.response.contentAsString
        val session = objectMapper.readValue(sessionJson, TestSession::class.java)
        
        mockMvc.perform(post("/api/session/${session.sessionId}/training"))
            .andExpect(status().isOk)
        
        // Get current trial
        mockMvc.perform(get("/api/session/${session.sessionId}/trial"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.trialId").exists())
            .andExpect(jsonPath("$.stimulus").exists())
            .andExpect(jsonPath("$.taskType").exists())
    }
    
    @Test
    fun `should record response`() {
        // First create a session and start training
        val sessionResponse = mockMvc.perform(post("/api/session")
                .param("participantId", "test-participant")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andReturn()
        
        val sessionJson = sessionResponse.response.contentAsString
        val session = objectMapper.readValue(sessionJson, TestSession::class.java)
        
        mockMvc.perform(post("/api/session/${session.sessionId}/training"))
            .andExpect(status().isOk)
        
        // Get current trial to know the correct response
        val trialResponse = mockMvc.perform(get("/api/session/${session.sessionId}/trial"))
            .andExpect(status().isOk)
            .andReturn()
        
        val trialJson = trialResponse.response.contentAsString
        val trial = objectMapper.readValue(trialJson, Trial::class.java)
        
        // Record response
        val responseRequest = ResponseRequest(
            response = trial.stimulus.correctResponse,
            responseTime = 500L
        )
        
        mockMvc.perform(post("/api/session/${session.sessionId}/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(responseRequest)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.response").value(trial.stimulus.correctResponse.toString()))
            .andExpect(jsonPath("$.responseTime").value(500))
            .andExpect(jsonPath("$.isCorrect").value(true))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }
    
    @Test
    fun `should complete session and return results`() {
        // First create a session and start training
        val sessionResponse = mockMvc.perform(post("/api/session")
                .param("participantId", "test-participant")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andReturn()
        
        val sessionJson = sessionResponse.response.contentAsString
        val session = objectMapper.readValue(sessionJson, TestSession::class.java)
        
        mockMvc.perform(post("/api/session/${session.sessionId}/training"))
            .andExpect(status().isOk)
        
        // Complete a few trials
        repeat(5) {
            val trialResponse = mockMvc.perform(get("/api/session/${session.sessionId}/trial"))
                .andExpect(status().isOk)
                .andReturn()
            
            val trialJson = trialResponse.response.contentAsString
            val trial = objectMapper.readValue(trialJson, Trial::class.java)
            
            val responseRequest = ResponseRequest(
                response = trial.stimulus.correctResponse,
                responseTime = 500L + (it * 10)
            )
            
            mockMvc.perform(post("/api/session/${session.sessionId}/response")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(responseRequest)))
                .andExpect(status().isOk)
        }
        
        // Complete session
        mockMvc.perform(post("/api/session/${session.sessionId}/complete"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionId").value(session.sessionId))
            .andExpect(jsonPath("$.totalTrials").value(5))
            .andExpect(jsonPath("$.correctTrials").value(5))
            .andExpect(jsonPath("$.accuracy").value(1.0))
            .andExpect(jsonPath("$.averageRT").exists())
            .andExpect(jsonPath("$.switchCost").exists())
            .andExpect(jsonPath("$.taskInterference").exists())
    }
    
    @Test
    fun `should get session details`() {
        // First create a session
        val sessionResponse = mockMvc.perform(post("/api/session")
                .param("participantId", "test-participant")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk)
            .andReturn()
        
        val sessionJson = sessionResponse.response.contentAsString
        val session = objectMapper.readValue(sessionJson, TestSession::class.java)
        
        // Get session details
        mockMvc.perform(get("/api/session/${session.sessionId}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionId").value(session.sessionId))
            .andExpect(jsonPath("$.participantId").value("test-participant"))
            .andExpect(jsonPath("$.currentPhase").value("INSTRUCTIONS"))
    }
    
    @Test
    fun `should return null for non-existent session`() {
        mockMvc.perform(get("/api/session/non-existent-id"))
            .andExpect(status().isOk)
            .andExpect(content().string(""))
    }
}
