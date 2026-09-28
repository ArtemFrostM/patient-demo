package com.patient.demo;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PatientDemoApplication {

    public static void main(String[] args) {
        // Must run before any DataSource is created: the PostgreSQL driver sends the JVM
        // default zone as the "TimeZone" startup parameter. On Windows the JDK may resolve
        // it to a legacy id (e.g. "Europe/Kiev"), which PostgreSQL 17 rejects with
        // `FATAL: invalid value for parameter "TimeZone"` since it was renamed in tzdata 2022b.
        // Pinning UTC also keeps behaviour identical across IDE, Gradle, Docker and CI.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication.run(PatientDemoApplication.class, args);
    }
}
