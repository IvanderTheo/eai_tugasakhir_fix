package com.example.paymentservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip filter for OPTIONS requests
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        
        // Skip filter for public endpoints (no JWT required)
        if (path.startsWith("/api/auth/") ||
            path.startsWith("/health") ||
            path.startsWith("/actuator/") ||
            path.startsWith("/swagger-ui/") ||
            path.startsWith("/v3/api-docs")) {
            logger.debug("Skipping JWT filter for public endpoint: {} {}", request.getMethod(), path);
            return true;
        }
        
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.getUsernameFromToken(jwt);
                
                List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = new ArrayList<>();
                try {
                    io.jsonwebtoken.Claims claims = jwtUtil.getClaimsFromToken(jwt);
                    Object rolesClaim = claims.get("roles");
                    if (rolesClaim instanceof java.util.Collection) {
                        for (Object roleObj : (java.util.Collection<?>) rolesClaim) {
                            if (roleObj instanceof java.util.Map) {
                                java.util.Map<?, ?> roleMap = (java.util.Map<?, ?>) roleObj;
                                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority((String) roleMap.get("authority")));
                            } else if (roleObj instanceof String) {
                                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority((String) roleObj));
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse roles claim: {}", e.getMessage());
                }

                if (authorities.isEmpty()) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
                }
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

