package com.patient.demo.refdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ref-data")
public record RefDataProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
