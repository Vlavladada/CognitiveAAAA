package com.cognitive.aaaa.demo.domain

class TaskSwitchingCognitiveTest {

    companion object {
        private const val TRAINING_SIZE = 3
        private const val TEST_SET_SIZE = 3
        private const val NUMBER_OF_SETS = 3
        private const val SET_PAUSE_DURATION_SECONDS = 120
    }



    enum class Shape {
        CIRCLE, RECTANGLE
    }

    enum class Color {
        BLUE, YELLOW
    }

    enum class Response {
        LEFT, RIGHT
    }

    enum class Congruency {
        CONGRUENT, INCONGRUENT
    }

    enum class TaskType {
        COLOR, SHAPE
    }

    enum class TrialStatus {
        PENDING, PRESENTED, RESPONDED, COMPLETED
    }
}