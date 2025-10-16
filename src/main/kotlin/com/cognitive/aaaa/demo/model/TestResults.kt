package com.cognitive.aaaa.demo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "test_results")
data class TestResults(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    val session: TestSession,
    
    @Column(nullable = false)
    val totalTrials: Int,
    
    @Column(nullable = false)
    val correctTrials: Int,
    
    @Column(nullable = false)
    val accuracy: Double, // percentage
    
    @Column(nullable = false)
    val averageResponseTime: Double, // in milliseconds
    
    @Column(nullable = false)
    val switchCost: Double, // difference between switch and repeat trials
    
    @Column(nullable = false)
    val taskInterference: Double, // difference between incongruent and congruent trials
    
    @Column(nullable = false)
    val errorCount: Int,
    
    @Column(nullable = false)
    val colorTaskAccuracy: Double,
    
    @Column(nullable = false)
    val shapeTaskAccuracy: Double,
    
    @Column(nullable = false)
    val colorTaskAvgRT: Double,
    
    @Column(nullable = false)
    val shapeTaskAvgRT: Double,
    
    @Column(nullable = false)
    val congruentAvgRT: Double,
    
    @Column(nullable = false)
    val incongruentAvgRT: Double,
    
    @Column(nullable = false)
    val switchAvgRT: Double,
    
    @Column(nullable = false)
    val repeatAvgRT: Double,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = 0,
        session = TestSession(),
        totalTrials = 0,
        correctTrials = 0,
        accuracy = 0.0,
        averageResponseTime = 0.0,
        switchCost = 0.0,
        taskInterference = 0.0,
        errorCount = 0,
        colorTaskAccuracy = 0.0,
        shapeTaskAccuracy = 0.0,
        colorTaskAvgRT = 0.0,
        shapeTaskAvgRT = 0.0,
        congruentAvgRT = 0.0,
        incongruentAvgRT = 0.0,
        switchAvgRT = 0.0,
        repeatAvgRT = 0.0,
        createdAt = LocalDateTime.now()
    )
}
