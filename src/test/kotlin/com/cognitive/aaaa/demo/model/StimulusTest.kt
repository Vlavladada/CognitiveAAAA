package com.cognitive.aaaa.demo.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StimulusTest {
    
    @Test
    fun `should create congruent color stimulus correctly`() {
        val stimulus = Stimulus(
            shape = Shape.CIRCLE,
            color = Color.YELLOW,
            correctResponse = Response.LEFT,
            taskType = TaskType.COLOR,
            congruency = Congruency.CONGRUENT
        )
        
        assertEquals(Shape.CIRCLE, stimulus.shape)
        assertEquals(Color.YELLOW, stimulus.color)
        assertEquals(Response.LEFT, stimulus.correctResponse)
        assertEquals(TaskType.COLOR, stimulus.taskType)
        assertEquals(Congruency.CONGRUENT, stimulus.congruency)
    }
    
    @Test
    fun `should create incongruent shape stimulus correctly`() {
        val stimulus = Stimulus(
            shape = Shape.RECTANGLE,
            color = Color.BLUE,
            correctResponse = Response.RIGHT,
            taskType = TaskType.SHAPE,
            congruency = Congruency.INCONGRUENT
        )
        
        assertEquals(Shape.RECTANGLE, stimulus.shape)
        assertEquals(Color.BLUE, stimulus.color)
        assertEquals(Response.RIGHT, stimulus.correctResponse)
        assertEquals(TaskType.SHAPE, stimulus.taskType)
        assertEquals(Congruency.INCONGRUENT, stimulus.congruency)
    }
}
