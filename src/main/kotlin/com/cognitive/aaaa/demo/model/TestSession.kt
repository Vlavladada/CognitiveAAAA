package com.cognitive.aaaa.demo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "test_sessions")
data class TestSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val sessionId: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,
    
    val participantId: String? = null,
    
    @Column(nullable = false)
    val sessionNumber: Int = 1,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: SessionStatus = SessionStatus.CREATED,
    
    @Column(nullable = false)
    val startTime: LocalDateTime = LocalDateTime.now(),
    
    val endTime: LocalDateTime? = null,
    
    @Column(nullable = false)
    val totalTrials: Int = 0,
    
    @Column(nullable = false)
    val completedTrials: Int = 0,
    
    @Column(nullable = false)
    val isCompleted: Boolean = false,
    
    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val trials: List<Trial> = emptyList()
) {
    constructor() : this(
        id = 0,
        sessionId = "",
        user = null,
        participantId = null,
        sessionNumber = 1,
        status = SessionStatus.CREATED,
        startTime = LocalDateTime.now(),
        endTime = null,
        totalTrials = 0,
        completedTrials = 0,
        isCompleted = false,
        trials = emptyList()
    )
}

enum class SessionStatus {
    CREATED, TRAINING, TESTING, COMPLETED, ABANDONED
}
