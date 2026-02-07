package com.boilerplate.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret = System.getenv().getOrDefault(
        "JWT_SECRET",
        "default-secret-key-change-in-production-must-be-at-least-256-bits-long"
    );
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;
    private Long rememberMeExpiration;
}
