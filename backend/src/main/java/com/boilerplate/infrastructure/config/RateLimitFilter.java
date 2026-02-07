package com.boilerplate.infrastructure.config;

import com.boilerplate.presentation.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/register".equals(request.getRequestURI())) {

            String clientIp = getClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

            if (!bucket.tryConsume(1)) {
                throw new RateLimitExceededException("Too many registration attempts. Please try again later.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
            5,
            Refill.intervally(5, Duration.ofHours(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
