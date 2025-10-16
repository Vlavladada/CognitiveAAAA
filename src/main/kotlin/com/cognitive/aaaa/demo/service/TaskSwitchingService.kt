package com.cognitive.aaaa.demo.service

import com.cognitive.aaaa.demo.model.*
import com.cognitive.aaaa.demo.repository.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class TaskSwitchingService(
    private val testSessionRepository: TestSessionRepository,
    private val trialRepository: TrialRepository,
    private val testResultsRepository: TestResultsRepository,
    private val userRepository: UserRepository
) {
    
    fun createSession(user: User? = null, participantId: String? = null): TestSession {
        val sessionId = UUID.randomUUID().toString()
        val sessionNumber = if (user != null) {
            testSessionRepository.findByUserSupabaseUserIdOrderByStartTimeDesc(user.supabaseUserId).size + 1
        } else 1
        
        val session = TestSession(
            sessionId = sessionId,
            user = user,
            participantId = participantId,
            sessionNumber = sessionNumber,
            status = SessionStatus.CREATED,
            startTime = LocalDateTime.now(),
            totalTrials = 0,
            completedTrials = 0,
            isCompleted = false
        )
        
        return testSessionRepository.save(session)
    }
    
    fun startTraining(sessionId: String): List<Trial> {
        val session = testSessionRepository.findBySessionId(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val trainingTrials = generateTrials(session, isTraining = true)
        val savedTrials = trialRepository.saveAll(trainingTrials)
        
        val updatedSession = session.copy(
            status = SessionStatus.TRAINING,
            totalTrials = trainingTrials.size
        )
        testSessionRepository.save(updatedSession)
        
        return savedTrials
    }
    
    fun startTest(sessionId: String): List<Trial> {
        val session = testSessionRepository.findBySessionId(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val testTrials = generateTrials(session, isTraining = false)
        val savedTrials = trialRepository.saveAll(testTrials)
        
        val updatedSession = session.copy(
            status = SessionStatus.TESTING,
            totalTrials = session.totalTrials + testTrials.size
        )
        testSessionRepository.save(updatedSession)
        
        return savedTrials
    }
    
    fun getCurrentTrial(sessionId: String): Trial? {
        val session = testSessionRepository.findBySessionId(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val trials = trialRepository.findBySessionIdOrderByTrialNumber(session.id)
        return trials.find { it.status == TrialStatus.PENDING }
    }
    
    fun recordResponse(sessionId: String, response: Response, responseTime: Long): Trial? {
        val session = testSessionRepository.findBySessionId(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val currentTrial = getCurrentTrial(sessionId) ?: return null
        
        val isCorrect = response == currentTrial.correctResponse
        val updatedTrial = currentTrial.copy(
            userResponse = response,
            responseTimeMs = responseTime,
            responseTime = LocalDateTime.now(),
            isCorrect = isCorrect,
            status = TrialStatus.COMPLETED
        )
        
        val savedTrial = trialRepository.save(updatedTrial)
        
        // Update session progress
        val updatedSession = session.copy(
            completedTrials = session.completedTrials + 1
        )
        testSessionRepository.save(updatedSession)
        
        return savedTrial
    }
    
    fun completeSession(sessionId: String): TestResults {
        val session = testSessionRepository.findBySessionId(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val trials = trialRepository.findBySessionIdOrderByTrialNumber(session.id)
        val completedTrials = trials.filter { it.status == TrialStatus.COMPLETED }
        
        val results = calculateResults(session, completedTrials)
        val savedResults = testResultsRepository.save(results)
        
        val updatedSession = session.copy(
            status = SessionStatus.COMPLETED,
            endTime = LocalDateTime.now(),
            isCompleted = true
        )
        testSessionRepository.save(updatedSession)
        
        // Update user test count
        session.user?.let { user ->
            val updatedUser = user.copy(
                totalTestsCompleted = user.totalTestsCompleted + 1,
                updatedAt = LocalDateTime.now()
            )
            userRepository.save(updatedUser)
        }
        
        return savedResults
    }
    
    fun getSession(sessionId: String): TestSession? {
        return testSessionRepository.findBySessionId(sessionId).orElse(null)
    }
    
    fun getUserSessions(userId: String): List<TestSession> {
        return testSessionRepository.findByUserSupabaseUserIdAndIsCompletedTrueOrderByStartTimeDesc(userId)
    }
    
    private fun generateTrials(session: TestSession, isTraining: Boolean): List<Trial> {
        val trialCount = if (isTraining) 16 else 128 // Training: 16 trials, Test: 128 trials
        val trials = mutableListOf<Trial>()
        
        val shapes = Shape.values()
        val colors = Color.values()
        val taskTypes = TaskType.values()
        
        var previousTaskType: TaskType? = null
        
        // Generate trials with proper task switching test design
        for (i in 1..trialCount) {
            val taskType = if (isTraining) {
                // Training: alternate between tasks every 4 trials
                if ((i - 1) / 4 % 2 == 0) TaskType.COLOR else TaskType.SHAPE
            } else {
                // Test: 50% switch probability with balanced design
                if (previousTaskType == null || Math.random() < 0.5) {
                    taskTypes.random()
                } else {
                    previousTaskType
                }
            }
            
            // Ensure balanced stimulus presentation
            val shape = if (isTraining) {
                shapes[i % shapes.size]
            } else {
                shapes.random()
            }
            
            val color = if (isTraining) {
                colors[i % colors.size]
            } else {
                colors.random()
            }
            
            val correctResponse = when (taskType) {
                TaskType.COLOR -> when (color) {
                    Color.BLUE -> Response.RIGHT
                    Color.YELLOW -> Response.LEFT
                }
                TaskType.SHAPE -> when (shape) {
                    Shape.CIRCLE -> Response.LEFT
                    Shape.RECTANGLE -> Response.RIGHT
                }
            }
            
            val congruency = determineCongruency(taskType, shape, color)
            val isSwitchTrial = previousTaskType != null && previousTaskType != taskType
            
            val trial = Trial(
                session = session,
                trialNumber = i,
                taskType = taskType,
                stimulusShape = shape,
                stimulusColor = color,
                correctResponse = correctResponse,
                congruency = congruency,
                status = TrialStatus.PENDING,
                stimulusOnsetTime = LocalDateTime.now(),
                isSwitchTrial = isSwitchTrial
            )
            
            trials.add(trial)
            previousTaskType = taskType
        }
        
        return trials
    }
    
    private fun determineCongruency(_taskType: TaskType, shape: Shape, color: Color): Congruency {
        // Congruent: both tasks would give the same response
        // Incongruent: tasks would give different responses
        val colorResponse = when (color) {
            Color.BLUE -> Response.RIGHT
            Color.YELLOW -> Response.LEFT
        }
        val shapeResponse = when (shape) {
            Shape.CIRCLE -> Response.LEFT
            Shape.RECTANGLE -> Response.RIGHT
        }
        
        return if (colorResponse == shapeResponse) Congruency.CONGRUENT else Congruency.INCONGRUENT
    }
    
    private fun calculateResults(session: TestSession, trials: List<Trial>): TestResults {
        val correctTrials = trials.filter { it.isCorrect == true }
        val accuracy = if (trials.isNotEmpty()) correctTrials.size.toDouble() / trials.size * 100 else 0.0
        
        val averageResponseTime = if (correctTrials.isNotEmpty()) {
            correctTrials.mapNotNull { it.responseTimeMs }.average()
        } else 0.0
        
        // Calculate switch cost (difference between switch and repeat trials)
        val switchTrials = trials.filter { it.isSwitchTrial && it.isCorrect == true }
        val repeatTrials = trials.filter { !it.isSwitchTrial && it.isCorrect == true }
        
        val switchAvgRT = if (switchTrials.isNotEmpty()) {
            switchTrials.mapNotNull { it.responseTimeMs }.average()
        } else 0.0
        
        val repeatAvgRT = if (repeatTrials.isNotEmpty()) {
            repeatTrials.mapNotNull { it.responseTimeMs }.average()
        } else 0.0
        
        val switchCost = switchAvgRT - repeatAvgRT
        
        // Calculate task interference (difference between incongruent and congruent trials)
        val congruentTrials = trials.filter { it.congruency == Congruency.CONGRUENT && it.isCorrect == true }
        val incongruentTrials = trials.filter { it.congruency == Congruency.INCONGRUENT && it.isCorrect == true }
        
        val congruentAvgRT = if (congruentTrials.isNotEmpty()) {
            congruentTrials.mapNotNull { it.responseTimeMs }.average()
        } else 0.0
        
        val incongruentAvgRT = if (incongruentTrials.isNotEmpty()) {
            incongruentTrials.mapNotNull { it.responseTimeMs }.average()
        } else 0.0
        
        val taskInterference = incongruentAvgRT - congruentAvgRT
        
        // Calculate task-specific metrics
        val colorTrials = trials.filter { it.taskType == TaskType.COLOR && it.isCorrect == true }
        val shapeTrials = trials.filter { it.taskType == TaskType.SHAPE && it.isCorrect == true }
        
        val colorTaskAccuracy = if (trials.filter { it.taskType == TaskType.COLOR }.isNotEmpty()) {
            colorTrials.size.toDouble() / trials.filter { it.taskType == TaskType.COLOR }.size * 100
        } else 0.0
        
        val shapeTaskAccuracy = if (trials.filter { it.taskType == TaskType.SHAPE }.isNotEmpty()) {
            shapeTrials.size.toDouble() / trials.filter { it.taskType == TaskType.SHAPE }.size * 100
        } else 0.0
        
        val colorTaskAvgRT = if (colorTrials.isNotEmpty()) {
            colorTrials.mapNotNull { it.responseTimeMs }.average()
        } else 0.0
        
        val shapeTaskAvgRT = if (shapeTrials.isNotEmpty()) {
            shapeTrials.mapNotNull { it.responseTimeMs }.average()
        } else 0.0
        
        return TestResults(
            session = session,
            totalTrials = trials.size,
            correctTrials = correctTrials.size,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            switchCost = switchCost,
            taskInterference = taskInterference,
            errorCount = trials.size - correctTrials.size,
            colorTaskAccuracy = colorTaskAccuracy,
            shapeTaskAccuracy = shapeTaskAccuracy,
            colorTaskAvgRT = colorTaskAvgRT,
            shapeTaskAvgRT = shapeTaskAvgRT,
            congruentAvgRT = congruentAvgRT,
            incongruentAvgRT = incongruentAvgRT,
            switchAvgRT = switchAvgRT,
            repeatAvgRT = repeatAvgRT,
            createdAt = LocalDateTime.now()
        )
    }
}