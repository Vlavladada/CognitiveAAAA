package com.cognitive.aaaa.demo.controller

import com.cognitive.aaaa.demo.model.*
import com.cognitive.aaaa.demo.service.SupabaseService
import com.cognitive.aaaa.demo.service.AuthenticationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val supabaseService: SupabaseService,
    private val authenticationService: AuthenticationService
) {
    
    // User Management
    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<List<User>> {
        val users = supabaseService.getAllUsers()
        return ResponseEntity.ok(users)
    }
    
    @GetMapping("/users/{userId}")
    fun getUserById(@PathVariable userId: String): ResponseEntity<User?> {
        val user = supabaseService.getUserById(userId)
        return ResponseEntity.ok(user)
    }
    
    @PutMapping("/users/{userId}/role")
    fun updateUserRole(
        @PathVariable userId: String,
        @RequestBody roleUpdate: RoleUpdateRequest
    ): ResponseEntity<User> {
        val user = supabaseService.getUserById(userId)
            ?: throw IllegalArgumentException("User not found")
        
        val updatedUser = user.copy(role = roleUpdate.role)
        // In a real implementation, you'd save this back to Supabase
        return ResponseEntity.ok(updatedUser)
    }
    
    @PutMapping("/users/{userId}/status")
    fun updateUserStatus(
        @PathVariable userId: String,
        @RequestBody statusUpdate: StatusUpdateRequest
    ): ResponseEntity<User> {
        val user = supabaseService.getUserById(userId)
            ?: throw IllegalArgumentException("User not found")
        
        val updatedUser = user.copy(isActive = statusUpdate.isActive)
        // In a real implementation, you'd save this back to Supabase
        return ResponseEntity.ok(updatedUser)
    }
    
    // Test Session Management
    @GetMapping("/sessions")
    fun getAllSessions(): ResponseEntity<List<TestSession>> {
        val sessions = supabaseService.getAllSessions()
        return ResponseEntity.ok(sessions)
    }
    
    @GetMapping("/sessions/{sessionId}")
    fun getSessionById(@PathVariable sessionId: String): ResponseEntity<TestSession?> {
        val session = supabaseService.getSession(sessionId)
        return ResponseEntity.ok(session)
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    fun deleteSession(@PathVariable sessionId: String): ResponseEntity<Void> {
        // In a real implementation, you'd delete from Supabase
        return ResponseEntity.ok().build()
    }
    
    // Analytics and Statistics
    @GetMapping("/stats/overview")
    fun getOverviewStats(): ResponseEntity<AdminStats> {
        val users = supabaseService.getAllUsers()
        val sessions = supabaseService.getAllSessions()
        
        val totalUsers = users.size
        val activeUsers = users.count { it.isActive }
        val totalSessions = sessions.size
        val completedSessions = sessions.count { it.endTime != null }
        
        val stats = AdminStats(
            totalUsers = totalUsers.toLong(),
            activeUsers = activeUsers.toLong(),
            totalSessions = totalSessions.toLong(),
            completedSessions = completedSessions.toLong(),
            completionRate = if (totalSessions > 0) completedSessions.toDouble() / totalSessions else 0.0
        )
        
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/stats/users/activity")
    fun getUserActivityStats(
        @RequestParam(defaultValue = "7") days: Int
    ): ResponseEntity<List<UserActivityStats>> {
        val users = supabaseService.getAllUsers()
        val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
        
        val activityStats = users.map { user ->
            val sessions = supabaseService.getUserSessions(user.id)
            val recentSessions = sessions.filter { it.startTime.isAfter(cutoffDate) }
            
            UserActivityStats(
                userId = user.id,
                email = user.email,
                totalTests = user.totalTestsCompleted,
                recentTests = recentSessions.size,
                lastLogin = user.lastLogin,
                isActive = user.isActive
            )
        }
        
        return ResponseEntity.ok(activityStats)
    }
    
    @GetMapping("/stats/sessions/performance")
    fun getSessionPerformanceStats(): ResponseEntity<List<SessionPerformanceStats>> {
        val sessions = supabaseService.getAllSessions()
        val completedSessions = sessions.filter { it.endTime != null }
        
        val performanceStats = completedSessions.map { session ->
            val trials = session.trials.filter { it.status == TrialStatus.COMPLETED }
            val correctTrials = trials.filter { it.isCorrect == true }
            
            SessionPerformanceStats(
                sessionId = session.sessionId,
                userId = session.userId,
                participantId = session.participantId,
                startTime = session.startTime,
                endTime = session.endTime,
                totalTrials = trials.size,
                correctTrials = correctTrials.size,
                accuracy = if (trials.isNotEmpty()) correctTrials.size.toDouble() / trials.size else 0.0,
                averageResponseTime = if (correctTrials.isNotEmpty()) {
                    correctTrials.mapNotNull { it.responseTime }.average()
                } else 0.0
            )
        }
        
        return ResponseEntity.ok(performanceStats)
    }
    
    // System Management
    @PostMapping("/system/cleanup")
    fun cleanupOldSessions(
        @RequestParam(defaultValue = "30") daysOld: Int
    ): ResponseEntity<CleanupResult> {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld.toLong())
        val sessions = supabaseService.getAllSessions()
        val oldSessions = sessions.filter { it.startTime.isBefore(cutoffDate) && it.endTime == null }
        
        val result = CleanupResult(
            deletedSessions = oldSessions.size,
            cutoffDate = cutoffDate
        )
        
        return ResponseEntity.ok(result)
    }
}

// DTOs for admin endpoints
data class RoleUpdateRequest(val role: UserRole)
data class StatusUpdateRequest(val isActive: Boolean)

data class AdminStats(
    val totalUsers: Long,
    val activeUsers: Long,
    val totalSessions: Long,
    val completedSessions: Long,
    val completionRate: Double
)

data class UserActivityStats(
    val userId: String,
    val email: String,
    val totalTests: Int,
    val recentTests: Int,
    val lastLogin: LocalDateTime?,
    val isActive: Boolean
)

data class SessionPerformanceStats(
    val sessionId: String,
    val userId: String?,
    val participantId: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val totalTrials: Int,
    val correctTrials: Int,
    val accuracy: Double,
    val averageResponseTime: Double
)

data class CleanupResult(
    val deletedSessions: Int,
    val cutoffDate: LocalDateTime
)
