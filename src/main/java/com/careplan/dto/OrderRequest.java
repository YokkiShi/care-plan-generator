package com.careplan.dto;

public record OrderRequest(
        String firstName,
        String lastName,
        String mrn,
        String dob,
        String providerName,
        String npi,
        String medicationName,
        String primaryDiagnosis,
        String medicationHistory
) {}
