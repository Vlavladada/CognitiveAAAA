package com.cognitive.aaaa.demo.domain.model

import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Color
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Congruency
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Response
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.Shape
import com.cognitive.aaaa.demo.domain.TaskSwitchingCognitiveTest.TaskType.COLOR
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "trials")
data class Trial(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    val session: TestSession,

    @Column(nullable = false)
    val trialNumber: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val taskType: TaskSwitchingCognitiveTest.TaskType,

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

    val isSwitchTrial: Boolean = false,
) {
    constructor() : this(
        id = null,
        session = TestSession(),
        trialNumber = 0,
        taskType = COLOR,
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
