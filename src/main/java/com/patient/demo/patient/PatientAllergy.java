package com.patient.demo.patient;

import com.patient.demo.allergy.Allergy;
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
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patient_allergy")
public class PatientAllergy extends AuditableEntity {

    @EmbeddedId
    private PatientAllergyId id = new PatientAllergyId();

    @MapsId("patientId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @MapsId("allergyId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergy_id", nullable = false)
    private Allergy allergy;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "notes", length = 1000)
    private String notes;
}
