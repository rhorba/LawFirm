package com.boilerplate.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        filterChain.doFilter(requestWrapper, responseWrapper);

        long duration = System.currentTimeMillis() - startTime;

        logRequestResponse(requestWrapper, responseWrapper, duration);

        responseWrapper.copyBodyToResponse();
    }

    private void logRequestResponse(
        ContentCachingRequestWrapper request,
        ContentCachingResponseWrapper response,
        long duration
    ) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        log.info("HTTP {} {} - Status: {} - Duration: {}ms",
            method, uri, status, duration);

        if (log.isDebugEnabled()) {
            String headers = Collections.list(request.getHeaderNames()).stream()
                .map(headerName -> headerName + ": " + request.getHeader(headerName))
                .collect(Collectors.joining(", "));

            log.debug("Request Headers: {}", headers);
        }
    }
}
