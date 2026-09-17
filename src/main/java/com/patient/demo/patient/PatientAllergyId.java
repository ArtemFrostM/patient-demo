package com.patient.demo.patient;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PatientAllergyId implements Serializable {

    private UUID patientId;
    private UUID allergyId;
}
