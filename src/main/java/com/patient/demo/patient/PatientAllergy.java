package com.patient.demo.patient;

import com.patient.demo.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient_allergy")
public class PatientAllergy extends AuditableEntity {

    @EmbeddedId
    private PatientAllergyId id = new PatientAllergyId();

    @MapsId("patientId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "notes", length = 1000)
    private String notes;

    public PatientAllergy(Patient patient, UUID allergyId) {
        this.id = new PatientAllergyId(patient.getId(), allergyId);
        this.patient = patient;
    }

    public UUID getAllergyId() {
        return id.getAllergyId();
    }
}
