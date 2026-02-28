# Care Plan Generation System — Design Document

**Version:** 0.1  
**Status:** Draft  
**Audience:** Engineering, Product

---

## 1. Overview

### 1.1 Background

A specialty pharmacy (CVS) requires a tool to automatically generate patient care plans based on clinical records. Today, pharmacists spend **20–40 minutes per patient** creating these plans manually. The process is required for Medicare reimbursement and pharma compliance, and the team is backlogged due to staffing constraints.

### 1.2 Users

- **Primary users:** CVS medical assistants and pharmacists
- **Not in scope:** Patients do not interact with this system
- **Workflow:** Medical worker fills out a form → system generates a care plan → care plan is printed and given to the patient

### 1.3 Goals

| Goal | Description |
|------|-------------|
| Reduce manual effort | Automate care plan generation from ~30 min → <2 min per patient |
| Ensure compliance | Support Medicare reimbursement and pharma reporting requirements |
| Prevent data errors | Validate inputs and detect duplicates before they enter the system |
| Enable reporting | Export structured data for pharma reporting |

---

## 2. Core Concepts

### 2.1 Care Plan Scope

- **One care plan = one order = one medication**
- Each care plan must include:
  - Problem List / Drug Therapy Problems (DTPs)
  - Goals (SMART format)
  - Pharmacist Interventions / Plan
  - Monitoring Plan & Lab Schedule

### 2.2 Entities

| Entity | Key Fields |
|--------|-----------|
| **Patient** | First Name, Last Name, MRN (6-digit unique ID), DOB |
| **Provider** | Name, NPI (10-digit unique identifier) |
| **Order** | Patient MRN, Medication Name, Primary Diagnosis (ICD-10), Date |
| **Care Plan** | Generated text linked to one Order |

---

## 3. Functional Requirements

### 3.1 Web Form (Input)

Medical assistants enter the following fields:

| Field | Type | Validation |
|-------|------|-----------|
| Patient First Name | String | Required |
| Patient Last Name | String | Required |
| Patient MRN | 6-digit number | Required, unique format |
| Date of Birth | Date | Required |
| Referring Provider Name | String | Required |
| Referring Provider NPI | 10-digit number | Required, must match existing NPI if provider exists |
| Primary Diagnosis | ICD-10 code | Required, valid ICD-10 format |
| Medication Name | String | Required |
| Additional Diagnoses | List of ICD-10 codes | Optional, valid ICD-10 format each |
| Medication History | List of strings | Optional |
| Patient Records | String or PDF upload | Optional but recommended for care plan quality |

### 3.2 Duplicate Detection

| Scenario | Behavior | Reason |
|----------|----------|--------|
| Same patient + same medication + **same day** | ❌ **ERROR** — block submission | Definite duplicate submission |
| Same patient + same medication + **different day** | ⚠️ **WARNING** — allow with confirmation | Likely a refill |
| Same MRN + different name or DOB | ⚠️ **WARNING** — allow with confirmation | Possible data entry error |
| Same name + same DOB + different MRN | ⚠️ **WARNING** — allow with confirmation | Possible same patient, different record |
| Same NPI + different provider name | ❌ **ERROR** — must correct before saving | NPI is the authoritative provider identifier |

### 3.3 Care Plan Generation

- On form submission, call an LLM (via API) with structured patient data
- LLM generates a care plan text following the standard 4-section template
- Care plan is returned as a downloadable `.txt` file
- Care plan is linked to the order record in the database

### 3.4 Export for Pharma Reporting

- Export orders and associated care plans in a structured format (CSV or JSON)
- Filters: date range, medication, provider
- Must include all required fields for Medicare and pharma reporting

---

## 4. Technical Design

### 4.1 System Architecture

```
[Web Form (Frontend)]
        ↓
[API Server (Backend)]
    ├── Input Validation
    ├── Duplicate Detection
    ├── LLM Call (Care Plan Generation)
    └── Database (Patients, Providers, Orders, Care Plans)
        ↓
[File Download / Export]
```

### 4.2 Tech Stack (Recommended)

| Layer | Choice | Notes |
|-------|--------|-------|
| Frontend | React + TypeScript | Form validation, warnings/errors UI |
| Backend | Node.js (Express) or Python (FastAPI) | REST API |
| Database | PostgreSQL | Relational integrity, good for compliance audit trails |
| LLM | Claude API (claude-sonnet-4) | Care plan generation |
| File Export | Server-side text/CSV generation | `.txt` for care plans, `.csv` for reports |

### 4.3 Data Model (Simplified)

```sql
patients      (mrn PK, first_name, last_name, dob, created_at)
providers     (npi PK, name, created_at)
orders        (id PK, patient_mrn FK, provider_npi FK, medication, 
               primary_diagnosis, order_date, created_at)
care_plans    (id PK, order_id FK, content TEXT, generated_at)
diagnoses     (order_id FK, icd10_code)
med_history   (order_id FK, medication_entry)
```

### 4.4 LLM Prompt Design

The backend constructs a structured prompt containing:
- Patient demographics (de-identified where possible)
- Primary and secondary diagnoses
- Medication and history
- Patient records text (if provided)

The LLM is instructed to output a care plan with exactly 4 sections:
1. Problem List / Drug Therapy Problems
2. Goals (SMART)
3. Pharmacist Interventions / Plan
4. Monitoring Plan & Lab Schedule

---

## 5. Validation Rules

| Rule | Detail |
|------|--------|
| MRN format | Exactly 6 digits |
| NPI format | Exactly 10 digits |
| ICD-10 format | Regex: `^[A-Z][0-9]{2}(\.[0-9A-Z]{1,4})?$` |
| Provider consistency | If NPI exists in DB, name must match |
| Required fields | All fields marked Required above must be non-empty |
| Date validity | DOB must be a valid past date |

---

## 6. Non-Functional Requirements

| Requirement | Target |
|------------|--------|
| Security | PHI handled in compliance with HIPAA; no patient data logged to console or third-party services |
| Error handling | All errors return safe, user-friendly messages; no stack traces exposed to frontend |
| Modularity | Validation, LLM call, DB logic in separate modules |
| Test coverage | Unit tests for all validation rules and duplicate detection logic |
| Run out of the box | Single setup command; env vars documented in `.env.example` |
| LLM latency | Care plan generation target <30 seconds |

---

## 7. Out of Scope (v1)

- Patient-facing portal
- EHR/EMR direct integration
- Multi-language support
- Role-based access control (all medical staff have same access in v1)
- Care plan editing after generation

---

## 8. Open Questions

| # | Question | Owner |
|---|----------|-------|
| 1 | Which LLM provider and model to use? | Engineering |
| 2 | What is the exact export format required by pharma partners? | Product / Compliance |
| 3 | Does the system need an audit log of who generated each care plan? | Compliance |
| 4 | PDF upload: what max file size should we support? | Product |
| 5 | Should generated care plans be editable before download? | Product |

---

## 9. Example Input / Output

### Input (Patient Record)

```
Name: A.B. | MRN: 000123 | DOB: 1979-06-08 | Sex: Female
Primary Dx: Generalized myasthenia gravis (G70.01)
Medication: IVIG
Secondary Dx: Hypertension, GERD
Home Meds: Pyridostigmine, Prednisone, Lisinopril, Omeprazole
```

### Output (Care Plan — 4 sections)

```
Problem List / Drug Therapy Problems
- Need for rapid immunomodulation
- Risk of infusion-related reactions
- Risk of renal dysfunction or volume overload
- Risk of thromboembolic events
- Potential drug–drug interactions
- Patient education / adherence gap

Goals (SMART)
- Primary: Clinically meaningful improvement in muscle strength within 2 weeks
- Safety: No severe infusion reaction, no acute kidney injury
- Process: Complete full 2 g/kg course with documented monitoring

Pharmacist Interventions / Plan
- Dosing & Administration
- Premedication protocol
- Infusion rate titration
- Hydration & renal protection
- Adverse event management plan

Monitoring Plan & Lab Schedule
- Before infusion: CBC, BMP, baseline vitals
- During infusion: Vitals q15–30 min
- Post-course (3–7 days): BMP to assess renal function
```
