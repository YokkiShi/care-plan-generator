package com.careplan.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_mrn", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_npi", nullable = false)
    private Provider provider;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(name = "primary_diagnosis", nullable = false)
    private String primaryDiagnosis;

    @Column(name = "medication_history", columnDefinition = "TEXT")
    private String medicationHistory;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public String getPrimaryDiagnosis() { return primaryDiagnosis; }
    public void setPrimaryDiagnosis(String primaryDiagnosis) { this.primaryDiagnosis = primaryDiagnosis; }
    public String getMedicationHistory() { return medicationHistory; }
    public void setMedicationHistory(String medicationHistory) { this.medicationHistory = medicationHistory; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
