-- ============================================================
-- Care Plan Generator — Mock Data
-- ============================================================

-- 1. Patients
INSERT INTO patients (mrn, first_name, last_name, dob, created_at) VALUES
  ('000101', 'Jane',    'Smith',    '1978-04-15', NOW()),
  ('000202', 'Robert',  'Johnson',  '1965-11-30', NOW()),
  ('000303', 'Maria',   'Garcia',   '1990-07-22', NOW()),
  ('000404', 'David',   'Chen',     '1955-02-08', NOW()),
  ('000505', 'Emily',   'Williams', '1983-09-19', NOW())
ON CONFLICT (mrn) DO NOTHING;

-- 2. Providers
INSERT INTO providers (npi, name, created_at) VALUES
  ('1234567890', 'Dr. Sarah Kim',    NOW()),
  ('0987654321', 'Dr. Michael Park', NOW()),
  ('1122334455', 'Dr. Lisa Nguyen',  NOW())
ON CONFLICT (npi) DO NOTHING;

-- 3. Orders
INSERT INTO orders (patient_mrn, provider_npi, medication_name, primary_diagnosis, medication_history, created_at) VALUES
  ('000101', '1234567890', 'IVIG',        'G70.01', 'Pyridostigmine, Prednisone',          NOW()),
  ('000101', '1234567890', 'Rituximab',   'G35',    'Interferon beta, Natalizumab',         NOW()),
  ('000202', '0987654321', 'Adalimumab',  'M05.79', 'Methotrexate, Hydroxychloroquine',     NOW()),
  ('000303', '1122334455', 'Omalizumab',  'J45.51', 'Fluticasone, Montelukast',             NOW()),
  ('000404', '0987654321', 'Infliximab',  'K50.90', 'Mesalamine, Azathioprine',             NOW()),
  ('000505', '1234567890', 'Tocilizumab', 'M06.09', 'Prednisone, Methotrexate, Leflunomide',NOW())
ON CONFLICT DO NOTHING;

-- 4. Care Plans
INSERT INTO care_plans (order_id, content, status, generated_at) VALUES
  (1, '## 1. Problem List / Drug Therapy Problems
- Myasthenia Gravis (G70.01) with acute exacerbation
- IVIG indicated for short-term immunomodulation
- Risk of infusion-related reactions
- Thromboembolism risk with IVIG

## 2. Goals (SMART Format)
- Patient will show measurable MG symptom improvement within 2-4 weeks of IVIG
- Complete IVIG infusion without serious adverse events
- No thromboembolic events during and 30 days post-infusion

## 3. Pharmacist Interventions / Plan
- Verify IVIG dose: 1-2 g/kg total over 2-5 days
- Pre-medicate: Acetaminophen + Diphenhydramine 30 min before infusion
- Monitor IgA level prior to first infusion
- Ensure adequate hydration throughout infusion

## 4. Monitoring Plan & Lab Schedule
- CBC, CMP, IgA level — before first infusion
- Serum creatinine — baseline and 72h post-infusion
- Clinical assessment at each infusion visit
- Follow-up with neurology in 4 weeks', 'COMPLETED', NOW()),

  (2, '## 1. Problem List / Drug Therapy Problems
- Multiple Sclerosis (G35), relapsing-remitting
- Rituximab for B-cell depletion therapy
- Immunosuppression risk / infection surveillance required

## 2. Goals (SMART Format)
- Reduce relapse rate by ≥50% within 12 months of initiating Rituximab
- CD19 B-cell count < 1% within 4 weeks of infusion

## 3. Pharmacist Interventions / Plan
- Confirm Rituximab dose: 1000 mg IV x2 doses, 2 weeks apart
- Pre-medicate: Methylprednisolone 100mg IV + Acetaminophen + Diphenhydramine
- Screen for hepatitis B, TB, and live vaccine history before starting
- Update all vaccines at least 4 weeks prior to therapy

## 4. Monitoring Plan & Lab Schedule
- CBC with differential — monthly x3, then every 3 months
- Immunoglobulin levels (IgG, IgA, IgM) — every 6 months
- CD19 B-cell count — 4 weeks post-infusion
- MRI brain/spine — annually', 'COMPLETED', NOW()),

  (3, '## 1. Problem List / Drug Therapy Problems
- Rheumatoid Arthritis (M05.79), seropositive
- Adalimumab TNF-alpha inhibitor therapy
- Infection risk (TB reactivation, opportunistic infections)

## 2. Goals (SMART Format)
- Achieve DAS28 score < 2.6 within 6 months
- Reduce morning stiffness to < 15 minutes within 3 months

## 3. Pharmacist Interventions / Plan
- Verify Adalimumab dose: 40 mg SQ every other week
- Screen for latent TB (IGRA or PPD) before initiation
- Educate on injection technique and site rotation
- Counsel to avoid live vaccines during therapy

## 4. Monitoring Plan & Lab Schedule
- CBC, LFTs, CRP, ESR — baseline, then every 3 months
- TB screening — annually
- DAS28 score assessment — every 3 months
- Ophthalmology consult if on concurrent Hydroxychloroquine', 'COMPLETED', NOW()),

  (4, NULL, 'FAILED', NOW()),

  (5, '## 1. Problem List / Drug Therapy Problems
- Crohn''s Disease (K50.90), moderate-to-severe
- Infliximab biologic therapy
- Risk of infusion reactions and opportunistic infections

## 2. Goals (SMART Format)
- Achieve clinical remission (CDAI < 150) within 12 weeks
- Mucosal healing confirmed on colonoscopy at 1 year

## 3. Pharmacist Interventions / Plan
- Infliximab induction: 5 mg/kg IV at weeks 0, 2, 6; then every 8 weeks
- Pre-medicate with Acetaminophen ± Diphenhydramine ± Hydrocortisone
- Screen for TB, hepatitis B, and fungal infections before starting
- Educate patient on signs of infection and when to seek care

## 4. Monitoring Plan & Lab Schedule
- CBC, CMP, CRP, fecal calprotectin — baseline and every 3 months
- Infliximab trough levels and antibodies — at week 14
- Colonoscopy — at 1 year to assess mucosal healing
- Annual TB screening', 'COMPLETED', NOW()),

  (6, NULL, 'PENDING', NOW())
ON CONFLICT DO NOTHING;
