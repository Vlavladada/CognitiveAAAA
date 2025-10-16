package com.cognitive.aaaa.demo.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class TestSessionTest {
    
    @Test
    fun `should create test session with correct initial state`() {
        val session = TestSession(participantId = "test-participant")
        
        assertNotNull(session.sessionId)
        assertEquals("test-participant", session.participantId)
        assertNotNull(session.startTime)
        assertNull(session.endTime)
        assertEquals(TestPhase.INSTRUCTIONS, session.currentPhase)
        assertEquals(0, session.currentTrialIndex)
        assertTrue(session.trials.isEmpty())
        assertNull(session.previousTaskType)
    }
    
    @Test
    fun `should update test session state correctly`() {
        val session = TestSession(participantId = "test-participant")
        
        // Update session state
        session.currentPhase = TestPhase.TRAINING
        session.currentTrialIndex = 5
        session.endTime = LocalDateTime.now()
        session.previousTaskType = TaskType.COLOR
        
        assertEquals(TestPhase.TRAINING, session.currentPhase)
        assertEquals(5, session.currentTrialIndex)
        assertNotNull(session.endTime)
        assertEquals(TaskType.COLOR, session.previousTaskType)
    }
}

class TestResultsTest {
    
    @Test
    fun `should create test results with correct values`() {
        val results = TestResults(
            sessionId = "test-session",
            totalTrials = 50,
            correctTrials = 45,
            averageRT = 750.5,
            repeatTrialsRT = 700.0,
            switchTrialsRT = 800.0,
            congruentTrialsRT = 650.0,
            incongruentTrialsRT = 850.0,
            switchCost = 100.0,
            taskInterference = 200.0,
            accuracy = 0.9
        )
        
        assertEquals("test-session", results.sessionId)
        assertEquals(50, results.totalTrials)
        assertEquals(45, results.correctTrials)
        assertEquals(750.5, results.averageRT, 0.01)
        assertEquals(700.0, results.repeatTrialsRT, 0.01)
        assertEquals(800.0, results.switchTrialsRT, 0.01)
        assertEquals(650.0, results.congruentTrialsRT, 0.01)
        assertEquals(850.0, results.incongruentTrialsRT, 0.01)
        assertEquals(100.0, results.switchCost, 0.01)
        assertEquals(200.0, results.taskInterference, 0.01)
        assertEquals(0.9, results.accuracy, 0.01)
    }
}
