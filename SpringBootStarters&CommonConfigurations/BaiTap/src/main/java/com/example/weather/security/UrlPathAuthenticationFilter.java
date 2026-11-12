package com.example.weather.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Custom Filter to extract username and password from the URI path:
 * Pattern: /weather/{username}/{password}/{location}
 */
public class UrlPathAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UrlPathAuthenticationFilter.class);

    private final AuthenticationManager authenticationManager;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

    public UrlPathAuthenticationFilter(AuthenticationManager authenticationManager,
                                      AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationManager = authenticationManager;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;

        log.debug("UrlPathAuthenticationFilter processing path: {}", path);

        // Only process paths starting with /weather/
        if (path != null && path.startsWith("/weather/")) {
            String[] segments = path.substring("/weather/".length()).split("/", -1);

            if (segments.length >= 2 && !segments[0].trim().isEmpty() && !segments[1].trim().isEmpty()) {
                String rawUsername = segments[0];
                String rawPassword = segments[1];

                try {
                    String username = URLDecoder.decode(rawUsername, StandardCharsets.UTF_8);
                    String password = URLDecoder.decode(rawPassword, StandardCharsets.UTF_8);

                    log.debug("Authenticating username: {}", username);
                    UsernamePasswordAuthenticationToken authRequest =
                            new UsernamePasswordAuthenticationToken(username, password);

                    Authentication authResult = authenticationManager.authenticate(authRequest);

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authResult);
                    SecurityContextHolder.setContext(context);
                    securityContextRepository.saveContext(context, request, response);

                    log.debug("Authentication successful for user: {}, authorities: {}",
                            authResult.getName(), authResult.getAuthorities());
                } catch (AuthenticationException ex) {
                    log.warn("Authentication failed for path credentials: {}", ex.getMessage());
                    SecurityContextHolder.clearContext();
                    authenticationEntryPoint.commence(request, response, ex);
                    return;
                } catch (Exception ex) {
                    log.error("Error processing credentials from path", ex);
                    SecurityContextHolder.clearContext();
                    authenticationEntryPoint.commence(request, response,
                            new BadCredentialsException("Failed to process credentials from path", ex));
                    return;
                }
            } else {
                log.warn("Invalid weather URL path format: {}", path);
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response,
                        new BadCredentialsException("Invalid URI format: Expected /weather/$username/$password/$location"));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
