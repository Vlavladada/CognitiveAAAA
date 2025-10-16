package com.cognitive.aaaa.demo.service

import com.cognitive.aaaa.demo.model.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import java.time.LocalDateTime

class TaskSwitchingServiceTest {
    
    private lateinit var taskSwitchingService: TaskSwitchingService
    private lateinit var supabaseService: SupabaseService
    
    @BeforeEach
    fun setUp() {
        supabaseService = mockk<SupabaseService>()
        taskSwitchingService = TaskSwitchingService(supabaseService)
    }
    
    @Nested
    inner class SessionManagement {
        
        @Test
        fun `should create session with participant ID`() {
            val expectedSession = TestSession(participantId = "test-participant")
            
            every { supabaseService.createSession(null, "test-participant") }
                .returns(expectedSession)
            
            val session = taskSwitchingService.createSession(participantId = "test-participant")
            
            assertNotNull(session.sessionId)
            assertEquals("test-participant", session.participantId)
            assertEquals(TestPhase.INSTRUCTIONS, session.currentPhase)
            assertEquals(0, session.currentTrialIndex)
            assertTrue(session.trials.isEmpty())
        }
        
        @Test
        fun `should create session without participant ID`() {
            val expectedSession = TestSession()
            
            every { supabaseService.createSession(null, null) }
                .returns(expectedSession)
            
            val session = taskSwitchingService.createSession()
            
            assertNotNull(session.sessionId)
            assertNull(session.participantId)
            assertEquals(TestPhase.INSTRUCTIONS, session.currentPhase)
        }
        
        @Test
        fun `should retrieve existing session`() {
            val expectedSession = TestSession(participantId = "test-participant")
            
            every { supabaseService.getSession("test-session-id") }
                .returns(expectedSession)
            
            val retrievedSession = taskSwitchingService.getSession("test-session-id")
            
            assertNotNull(retrievedSession)
            assertEquals(expectedSession.sessionId, retrievedSession!!.sessionId)
            assertEquals("test-participant", retrievedSession.participantId)
        }
        
        @Test
        fun `should return null for non-existent session`() {
            every { supabaseService.getSession("non-existent-id") }
                .returns(null)
            
            val retrievedSession = taskSwitchingService.getSession("non-existent-id")
            
            assertNull(retrievedSession)
        }
    }
    
    @Nested
    inner class TestExecution {
        
        @Test
        fun `should start training phase correctly`() {
            val expectedTrials = listOf(
                Trial(sessionId = "test-session", stimulus = createTestStimulus(), taskType = TaskType.COLOR, isTaskSwitch = false),
                Trial(sessionId = "test-session", stimulus = createTestStimulus(), taskType = TaskType.SHAPE, isTaskSwitch = true)
            )
            
            every { supabaseService.startTraining("test-session") }
                .returns(expectedTrials)
            
            val trials = taskSwitchingService.startTraining("test-session")
            
            assertEquals(2, trials.size)
        }
        
        @Test
        fun `should start test phase correctly`() {
            val expectedTrials = listOf(
                Trial(sessionId = "test-session", stimulus = createTestStimulus(), taskType = TaskType.COLOR, isTaskSwitch = false),
                Trial(sessionId = "test-session", stimulus = createTestStimulus(), taskType = TaskType.SHAPE, isTaskSwitch = true)
            )
            
            every { supabaseService.startTest("test-session") }
                .returns(expectedTrials)
            
            val trials = taskSwitchingService.startTest("test-session")
            
            assertEquals(2, trials.size)
        }
        
        @Test
        fun `should get current trial correctly`() {
            val expectedTrial = Trial(sessionId = "test-session", stimulus = createTestStimulus(), taskType = TaskType.COLOR, isTaskSwitch = false)
            
            every { supabaseService.getCurrentTrial("test-session") }
                .returns(expectedTrial)
            
            val currentTrial = taskSwitchingService.getCurrentTrial("test-session")
            
            assertNotNull(currentTrial)
            assertEquals(expectedTrial.trialId, currentTrial!!.trialId)
        }
        
        @Test
        fun `should return null when no more trials`() {
            every { supabaseService.getCurrentTrial("test-session") }
                .returns(null)
            
            val currentTrial = taskSwitchingService.getCurrentTrial("test-session")
            
            assertNull(currentTrial)
        }
    }
    
    @Nested
    inner class ResponseRecording {
        
        @Test
        fun `should record correct response`() {
            val expectedTrial = Trial(
                sessionId = "test-session", 
                stimulus = createTestStimulus(), 
                taskType = TaskType.COLOR, 
                isTaskSwitch = false
            )
            expectedTrial.response = Response.LEFT
            expectedTrial.responseTime = 500L
            expectedTrial.isCorrect = true
            expectedTrial.status = TrialStatus.COMPLETED
            
            every { supabaseService.recordResponse("test-session", Response.LEFT, 500L) }
                .returns(expectedTrial)
            
            val recordedTrial = taskSwitchingService.recordResponse("test-session", Response.LEFT, 500L)
            
            assertNotNull(recordedTrial)
            assertEquals(Response.LEFT, recordedTrial!!.response)
            assertEquals(500L, recordedTrial.responseTime)
            assertTrue(recordedTrial.isCorrect!!)
            assertEquals(TrialStatus.COMPLETED, recordedTrial.status)
        }
        
        @Test
        fun `should record incorrect response`() {
            val expectedTrial = Trial(
                sessionId = "test-session", 
                stimulus = createTestStimulus(), 
                taskType = TaskType.COLOR, 
                isTaskSwitch = false
            )
            expectedTrial.response = Response.RIGHT
            expectedTrial.responseTime = 500L
            expectedTrial.isCorrect = false
            expectedTrial.status = TrialStatus.COMPLETED
            
            every { supabaseService.recordResponse("test-session", Response.RIGHT, 500L) }
                .returns(expectedTrial)
            
            val recordedTrial = taskSwitchingService.recordResponse("test-session", Response.RIGHT, 500L)
            
            assertNotNull(recordedTrial)
            assertEquals(Response.RIGHT, recordedTrial!!.response)
            assertEquals(500L, recordedTrial.responseTime)
            assertFalse(recordedTrial.isCorrect!!)
            assertEquals(TrialStatus.COMPLETED, recordedTrial.status)
        }
        
        @Test
        fun `should throw exception for non-existent session`() {
            every { supabaseService.recordResponse("non-existent-id", Response.LEFT, 500L) }
                .throws(IllegalArgumentException("Session not found"))
            
            assertThrows(IllegalArgumentException::class.java) {
                taskSwitchingService.recordResponse("non-existent-id", Response.LEFT, 500L)
            }
        }
    }
    
    @Nested
    inner class ResultsCalculation {
        
        @Test
        fun `should calculate results correctly for completed session`() {
            val expectedResults = TestResults(
                sessionId = "test-session",
                totalTrials = 10,
                correctTrials = 8,
                averageRT = 500.0,
                repeatTrialsRT = 450.0,
                switchTrialsRT = 550.0,
                congruentTrialsRT = 400.0,
                incongruentTrialsRT = 600.0,
                switchCost = 100.0,
                taskInterference = 200.0,
                accuracy = 0.8
            )
            
            every { supabaseService.completeSession("test-session") }
                .returns(expectedResults)
            
            val results = taskSwitchingService.completeSession("test-session")
            
            assertNotNull(results)
            assertEquals("test-session", results.sessionId)
            assertEquals(10, results.totalTrials)
            assertEquals(8, results.correctTrials)
            assertEquals(0.8, results.accuracy, 0.01)
        }
        
        @Test
        fun `should throw exception for non-existent session completion`() {
            every { supabaseService.completeSession("non-existent-id") }
                .throws(IllegalArgumentException("Session not found"))
            
            assertThrows(IllegalArgumentException::class.java) {
                taskSwitchingService.completeSession("non-existent-id")
            }
        }
    }
    
    @Nested
    inner class UserSessions {
        
        @Test
        fun `should get user sessions`() {
            val expectedSessions = listOf(
                TestSession(sessionId = "session1", userId = "user1"),
                TestSession(sessionId = "session2", userId = "user1")
            )
            
            every { supabaseService.getUserSessions("user1") }
                .returns(expectedSessions)
            
            val sessions = taskSwitchingService.getUserSessions("user1")
            
            assertEquals(2, sessions.size)
            assertEquals("session1", sessions[0].sessionId)
            assertEquals("session2", sessions[1].sessionId)
        }
    }
    
    private fun createTestStimulus(): Stimulus {
        return Stimulus(
            shape = Shape.CIRCLE,
            color = Color.YELLOW,
            correctResponse = Response.LEFT,
            taskType = TaskType.COLOR,
            congruency = Congruency.CONGRUENT
        )
    }
}