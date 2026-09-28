package com.patient.demo;

import org.springframework.boot.SpringApplication;

public class TestPatientDemoApplication {

    public static void main(String[] args) {
        SpringApplication.from(PatientDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
