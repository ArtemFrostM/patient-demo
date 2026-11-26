package com.patient.demo.patient;

import com.patient.demo.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient_allergy")
public class PatientAllergy extends AuditableEntity implements Persistable<PatientAllergyId> {

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

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    public PatientAllergy(Patient patient, UUID allergyId) {
        this.id = new PatientAllergyId(patient.getId(), allergyId);
        this.patient = patient;
    }

    public UUID getAllergyId() {
        return id.getAllergyId();
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
