package com.cognitive.aaaa.demo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "trials")
data class Trial(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: TestSession,
    
    @Column(nullable = false)
    val trialNumber: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val taskType: TaskType,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val stimulusShape: Shape,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val stimulusColor: Color,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val correctResponse: Response,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val congruency: Congruency,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TrialStatus = TrialStatus.PENDING,
    
    val userResponse: Response? = null,
    
    val responseTimeMs: Long? = null, // in milliseconds
    
    val isCorrect: Boolean? = null,
    
    @Column(nullable = false)
    val stimulusOnsetTime: LocalDateTime = LocalDateTime.now(),
    
    val responseTime: LocalDateTime? = null,
    
    val isSwitchTrial: Boolean = false, // true if task type differs from previous trial
) {
    constructor() : this(
        id = 0,
        session = TestSession(),
        trialNumber = 0,
        taskType = TaskType.COLOR,
        stimulusShape = Shape.CIRCLE,
        stimulusColor = Color.BLUE,
        correctResponse = Response.LEFT,
        congruency = Congruency.CONGRUENT,
        status = TrialStatus.PENDING,
        userResponse = null,
        responseTimeMs = null,
        isCorrect = null,
        stimulusOnsetTime = LocalDateTime.now(),
        responseTime = null,
        isSwitchTrial = false
    )
}

enum class TrialStatus {
    PENDING, PRESENTED, RESPONDED, COMPLETED
}
