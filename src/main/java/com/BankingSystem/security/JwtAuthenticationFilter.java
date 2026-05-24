package com.BankingSystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Authorization header or doesn't start with Bearer
        // Pass through - Spring Security will decide if this endpoint needs auth
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the token - "Bearer " is 7 characters so skip them
        final String jwt = authHeader.substring(7);

        // Extract email from token
        final String userEmail = jwtService.extractUsername(jwt);

        // UserEmail is not null AND User is not already authenticated
        if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load User form Database
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Validate token against the loaded user
            if(jwtService.isTokenValid(jwt, userDetails)) {

                // Create authentication token for spring security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Tell Spring Security - this user is authenticated
                // for the duration of this request
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Always pass request to next filter regardless
        filterChain.doFilter(request, response);
    }
}