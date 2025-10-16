package com.cognitive.aaaa.demo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/config")
class ConfigController(
    @Value("\${supabase.url}") private val supabaseUrl: String,
    @Value("\${supabase.anon-key}") private val supabaseAnonKey: String,
    @Value("\${app.version:1.0.0}") private val appVersion: String,
    @Value("\${app.environment:production}") private val environment: String
) {
    
    @GetMapping("/public")
    fun getPublicConfig(): Map<String, Any> {
        return mapOf(
            "supabaseUrl" to supabaseUrl,
            "supabaseAnonKey" to supabaseAnonKey,
            "appVersion" to appVersion,
            "environment" to environment
        )
    }
    
    @GetMapping("/client")
    fun getClientConfig(): Map<String, Any> {
        return mapOf(
            "appVersion" to appVersion,
            "environment" to environment,
            "features" to mapOf(
                "authentication" to true,
                "adminPanel" to true,
                "analytics" to true,
                "taskSwitching" to true
            ),
            "timing" to mapOf(
                "cueDisplayTime" to 350,
                "cueDelay" to 750,
                "interTrialInterval" to 1000,
                "responseTimeout" to 2000
            )
        )
    }
}