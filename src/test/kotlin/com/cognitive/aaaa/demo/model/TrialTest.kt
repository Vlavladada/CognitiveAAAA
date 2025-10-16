package com.cognitive.aaaa.demo.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class TrialTest {
    
    @Test
    fun `should create trial with correct initial state`() {
        val stimulus = Stimulus(
            shape = Shape.CIRCLE,
            color = Color.YELLOW,
            correctResponse = Response.LEFT,
            taskType = TaskType.COLOR,
            congruency = Congruency.CONGRUENT
        )
        
        val trial = Trial(
            trialId = "test-trial-1",
            sessionId = "test-session-1",
            stimulus = stimulus,
            taskType = TaskType.COLOR,
            isTaskSwitch = false,
            startTime = LocalDateTime.now()
        )
        
        assertEquals("test-trial-1", trial.trialId)
        assertEquals(stimulus, trial.stimulus)
        assertEquals(TaskType.COLOR, trial.taskType)
        assertFalse(trial.isTaskSwitch)
        assertNull(trial.responseTime)
        assertNull(trial.response)
        assertNull(trial.isCorrect)
        assertEquals(TrialStatus.PENDING, trial.status)
    }
    
    @Test
    fun `should update trial status correctly`() {
        val stimulus = Stimulus(
            shape = Shape.CIRCLE,
            color = Color.YELLOW,
            correctResponse = Response.LEFT,
            taskType = TaskType.COLOR,
            congruency = Congruency.CONGRUENT
        )
        
        val trial = Trial(
            trialId = "test-trial-1",
            sessionId = "test-session-1",
            stimulus = stimulus,
            taskType = TaskType.COLOR,
            isTaskSwitch = false,
            startTime = LocalDateTime.now()
        )
        
        // Simulate trial completion
        trial.responseTime = 500L
        trial.response = Response.LEFT
        trial.isCorrect = true
        trial.status = TrialStatus.COMPLETED
        
        assertEquals(500L, trial.responseTime)
        assertEquals(Response.LEFT, trial.response)
        assertTrue(trial.isCorrect!!)
        assertEquals(TrialStatus.COMPLETED, trial.status)
    }
}
