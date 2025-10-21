package com.cognitive.aaaa.demo.infra.config

import com.cognitive.aaaa.demo.infra.service.SupabaseAuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SupabaseAuthenticationFilter(
    private val supabaseAuthService: SupabaseAuthService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val userId = request.getHeader("X-User-ID")
            println("SupabaseAuthenticationFilter: X-User-ID header = $userId")
            
            if (userId != null) {
                val user = supabaseAuthService.getUserBySupabaseId(userId)
                println("SupabaseAuthenticationFilter: Found user = $user")
                
                if (user != null) {
                    val authentication = UsernamePasswordAuthenticationToken(
                        user.email, // Use email as principal
                        null, // No credentials needed
                        emptyList() // No authorities for now
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                    println("SupabaseAuthenticationFilter: Authentication set for user ${user.email}")
                } else {
                    println("SupabaseAuthenticationFilter: No user found for ID $userId")
                }
            } else {
                println("SupabaseAuthenticationFilter: No X-User-ID header found")
            }
        } catch (e: Exception) {
            // Log error but don't fail the request
            logger.error("Error in Supabase authentication filter", e)
            println("SupabaseAuthenticationFilter: Error = ${e.message}")
        }
        
        filterChain.doFilter(request, response)
    }
}
