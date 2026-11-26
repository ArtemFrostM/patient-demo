package com.patient.demo.refdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(Duration connectTimeout, Duration readTimeout) {
}
