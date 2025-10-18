package com.cognitive.aaaa.demo.application.service

import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Color
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Congruency
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Response
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Shape
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.TaskType
import com.cognitive.aaaa.demo.domain.model.SessionStatus
import com.cognitive.aaaa.demo.domain.model.TestResults
import com.cognitive.aaaa.demo.domain.model.TestSession
import com.cognitive.aaaa.demo.domain.model.Trial
import com.cognitive.aaaa.demo.domain.model.TrialStatus
import com.cognitive.aaaa.demo.domain.model.User
import com.cognitive.aaaa.demo.domain.repository.TestResultsRepository
import com.cognitive.aaaa.demo.domain.repository.TestSessionRepository
import com.cognitive.aaaa.demo.domain.repository.TrialRepository
import com.cognitive.aaaa.demo.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class TaskSwitchingService(
    private val testSessionRepository: TestSessionRepository,
    private val trialRepository: TrialRepository,
    private val testResultsRepository: TestResultsRepository,
    private val userRepository: UserRepository
) {
    
    fun createSession(user: User? = null): TestSession {
        val sessionNumber = if (user != null) {
            testSessionRepository.findByUserIdOrderByStartTimeDesc( user.id).size + 1
        } else 1
        
        val session = TestSession(
            id = null,
            user = user,
            sessionNumber = sessionNumber,
            status = SessionStatus.CREATED,
            startTime = LocalDateTime.now(),
            totalTrials = 0,
            completedTrials = 0,
            isCompleted = false
        )
        
        return testSessionRepository.save(session)
    }
    
    fun startTraining(sessionId: UUID): List<Trial> {
        val session =  testSessionRepository.findById(sessionId)
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
    
    fun startTest(sessionId: UUID): List<Trial> {
        val session =  testSessionRepository.findById(sessionId)
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
    
    fun getCurrentTrial(sessionId: UUID): Trial? {
        val session =  testSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val trials = trialRepository.findByIdOrderByTrialNumber(session.id!!)
        return trials.find { it.status == TrialStatus.PENDING }
    }
    
    fun recordResponse(sessionId: UUID, response: Response?, responseTime: Long): Trial? {
        val session =  testSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val currentTrial = getCurrentTrial(sessionId) ?: return null
        
        val isCorrect = response?.let { it == currentTrial.correctResponse } ?: false
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
    
    fun completeSession(sessionId: UUID): TestResults {
        val session =  testSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        val trials = trialRepository.findByIdOrderByTrialNumber(session.id!!)
        val completedTrials = trials.filter { it.status == TrialStatus.COMPLETED }
        
        val results = calculateResults(session, completedTrials)
        val savedResults = testResultsRepository.save(results)
        
        val updatedSession = session.copy(
            status = SessionStatus.COMPLETED,
            endTime = LocalDateTime.now(),
            isCompleted = true
        )
//        testSessionRepository.save(updatedSession)
        
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
    
    fun getSession(sessionId: UUID): TestSession? {
        return  testSessionRepository.findById(sessionId).orElse(null)
    }
    
    fun getUserSessions(userId: String): List<TestSession> {
        return testSessionRepository.findByUserIdAndIsCompletedTrueOrderByStartTimeDesc(userId)
    }
    
    private fun generateTrials(session: TestSession, isTraining: Boolean): List<Trial> {
        val trialCount = if (isTraining) 24 else 240 // Training: 24 trials, Test: 240 trials (psychologically valid)
        val trials = mutableListOf<Trial>()
        
        // Generate balanced trial sequence following psychological best practices
        val trialSequence = generateBalancedSequence(trialCount, isTraining)
        
        for (i in 1..trialCount) {
            val trialSpec = trialSequence[i - 1]
            
            val trial = Trial(
                id = UUID.randomUUID(),
                session = session,
                trialNumber = i,
                taskType = trialSpec.taskType,
                stimulusShape = trialSpec.shape,
                stimulusColor = trialSpec.color,
                correctResponse = trialSpec.correctResponse,
                congruency = trialSpec.congruency,
                status = TrialStatus.PENDING,
                stimulusOnsetTime = LocalDateTime.now(),
                isSwitchTrial = trialSpec.isSwitchTrial
            )
            
            trials.add(trial)
        }
        
        return trials
    }
    
    private data class TrialSpec(
        val taskType: TaskType,
        val shape: Shape,
        val color: Color,
        val correctResponse: Response,
        val congruency: Congruency,
        val isSwitchTrial: Boolean
    )
    
    private fun generateBalancedSequence(trialCount: Int, isTraining: Boolean): List<TrialSpec> {
        val trials = mutableListOf<TrialSpec>()
        val shapes = Shape.values()
        val colors = Color.values()
        val taskTypes = TaskType.values()
        
        if (isTraining) {
            // Training: Simple alternating pattern with clear examples
            for (i in 1..trialCount) {
                val taskType = if ((i - 1) / 6 % 2 == 0) TaskType.COLOR else TaskType.SHAPE
                val shape = shapes[(i - 1) % shapes.size]
                val color = colors[(i - 1) % colors.size]
                
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
                val isSwitchTrial = i > 1 && trials.last().taskType != taskType
                
                trials.add(TrialSpec(taskType, shape, color, correctResponse, congruency, isSwitchTrial))
            }
        } else {
            // Test: Balanced design with proper switch/repeat and congruency distribution
            val switchProbability = 0.5
            val congruentProbability = 0.5
            
            // Ensure balanced distribution
            val totalSwitches = (trialCount * switchProbability).toInt()
            val totalCongruent = (trialCount * congruentProbability).toInt()
            
            var switchCount = 0
            var congruentCount = 0
            var previousTaskType: TaskType? = null
            
            for (i in 1..trialCount) {
                // Determine if this should be a switch trial
                val remainingTrials = trialCount - i + 1
                val remainingSwitches = totalSwitches - switchCount
                val shouldSwitch = when {
                    remainingSwitches == remainingTrials -> true
                    remainingSwitches == 0 -> false
                    previousTaskType == null -> true
                    else -> Math.random() < (remainingSwitches.toDouble() / remainingTrials)
                }
                
                val taskType = if (shouldSwitch) {
                    taskTypes.random()
                } else {
                    previousTaskType ?: taskTypes.random()
                }
                
                if (shouldSwitch) switchCount++
                
                // Determine congruency
                val remainingCongruent = totalCongruent - congruentCount
                val shouldBeCongruent = when {
                    remainingCongruent == (trialCount - i + 1) -> true
                    remainingCongruent == 0 -> false
                    else -> Math.random() < (remainingCongruent.toDouble() / (trialCount - i + 1))
                }
                
                // Generate stimulus that matches desired congruency
                val (shape, color) = generateStimulusForCongruency(taskType, shouldBeCongruent)
                
                if (shouldBeCongruent) congruentCount++
                
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
                
                trials.add(TrialSpec(taskType, shape, color, correctResponse, congruency, isSwitchTrial))
                previousTaskType = taskType
            }
        }
        
        return trials
    }
    
    private fun generateStimulusForCongruency(taskType: TaskType, shouldBeCongruent: Boolean): Pair<Shape, Color> {
        val shapes = Shape.values()
        val colors = Color.values()
        
        return if (shouldBeCongruent) {
            // Generate congruent stimulus (both tasks give same response)
            when (taskType) {
                TaskType.COLOR -> {
                    // Choose shape first, then color that matches
                    val shape = shapes.random()
                    val color = when (shape) {
                        Shape.CIRCLE -> Color.YELLOW // Both would give LEFT
                        Shape.RECTANGLE -> Color.BLUE // Both would give RIGHT
                    }
                    Pair(shape, color)
                }
                TaskType.SHAPE -> {
                    // Choose color first, then shape that matches
                    val color = colors.random()
                    val shape = when (color) {
                        Color.YELLOW -> Shape.CIRCLE // Both would give LEFT
                        Color.BLUE -> Shape.RECTANGLE // Both would give RIGHT
                    }
                    Pair(shape, color)
                }
            }
        } else {
            // Generate incongruent stimulus (tasks give different responses)
            when (taskType) {
                TaskType.COLOR -> {
                    // Choose shape first, then color that conflicts
                    val shape = shapes.random()
                    val color = when (shape) {
                        Shape.CIRCLE -> Color.BLUE // Color says RIGHT, shape says LEFT
                        Shape.RECTANGLE -> Color.YELLOW // Color says LEFT, shape says RIGHT
                    }
                    Pair(shape, color)
                }
                TaskType.SHAPE -> {
                    // Choose color first, then shape that conflicts
                    val color = colors.random()
                    val shape = when (color) {
                        Color.YELLOW -> Shape.RECTANGLE // Color says LEFT, shape says RIGHT
                        Color.BLUE -> Shape.CIRCLE // Color says RIGHT, shape says LEFT
                    }
                    Pair(shape, color)
                }
            }
        }
    }
    
    private fun determineCongruency(taskType: TaskType, shape: Shape, color: Color): Congruency {
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
            id = UUID.randomUUID(),
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