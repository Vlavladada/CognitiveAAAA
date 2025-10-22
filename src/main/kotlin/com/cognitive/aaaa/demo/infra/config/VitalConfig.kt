package com.cognitive.aaaa.demo.infra.config

import com.vital.api.Vital
import com.vital.api.core.Environment
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VitalConfig {
    
    @Value("\${junction.api-key}")
    private lateinit var apiKey: String
    
    @Value("\${junction.environment}")
    private lateinit var environment: String
    
    @Bean
    fun vitalClient(): Vital {
        val environmentEnum =  Environment.SANDBOX
        return Vital.builder()
            .apiKey(apiKey)
            .environment(environmentEnum)
            .build()
    }
}
