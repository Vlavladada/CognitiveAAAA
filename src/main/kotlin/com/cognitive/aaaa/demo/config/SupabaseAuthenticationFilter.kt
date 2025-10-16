package com.cognitive.aaaa.demo.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class SupabaseAuthenticationFilter(
    private val supabaseJwtValidator: SupabaseJwtValidator
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val supabaseUser = supabaseJwtValidator.validateToken(token)
            
            if (supabaseUser != null) {
                // Determine user role from Supabase metadata or default to USER
                val userRole = determineUserRole(supabaseUser)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_$userRole"))
                
                val authentication = UsernamePasswordAuthenticationToken(
                    supabaseUser.id,
                    null,
                    authorities
                )
                
                // Store additional user info in the authentication details
                authentication.details = supabaseUser
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun determineUserRole(supabaseUser: SupabaseUser): String {
        // Check app_metadata for admin role
        val appMetadata = supabaseUser.appMetadata
        val userMetadata = supabaseUser.userMetadata
        
        // Check for admin role in metadata
        val adminRole = appMetadata?.get("role") as? String
            ?: userMetadata?.get("role") as? String
        
        return when (adminRole?.uppercase()) {
            "ADMIN", "SUPER_ADMIN" -> adminRole.uppercase()
            else -> "USER"
        }
    }
}
