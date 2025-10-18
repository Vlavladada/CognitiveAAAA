package com.cognitive.aaaa.demo.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "test_results")
data class TestResults(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

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
    val switchCost: Double, // difference between switch and repeat trials ms

    @Column(nullable = false)
    val taskInterference: Double, // difference between incongruent and congruent trials ms

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
    val congruentAvgrt: Double,

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
        id = null,
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
        congruentAvgrt = 0.0,
        incongruentAvgRT = 0.0,
        switchAvgRT = 0.0,
        repeatAvgRT = 0.0,
        createdAt = LocalDateTime.now()
    )
}