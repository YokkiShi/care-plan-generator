# Care Plan Generator — Claude Code Instructions

## Project Overview
A medical care plan generation system for CVS specialty pharmacy.
Medical assistants submit patient + order info → backend validates → LLM generates a care plan → user downloads it.

## Tech Stack
- **Backend:** Java 17 + Spring Boot 3.2.3
- **Frontend:** React + TypeScript (Day 2+)
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA + Hibernate
- **Build tool:** Maven
- **LLM:** Claude API (claude-sonnet-4)
- **Containerization:** Docker + Docker Compose (Day 4+)

## Project Structure
```
src/main/java/com/careplan/
├── controller/     # HTTP request handling only — no business logic here
├── service/        # All business logic (validation, LLM calls, duplicate detection)
├── repository/     # Spring Data JPA interfaces — database access only
├── model/          # JPA entities (@Entity classes)
├── dto/            # Request/Response objects (what frontend sends/receives)
└── exception/      # Custom exceptions (ValidationException, DuplicateException, etc.)
```

## Database Schema
```
patients   (mrn VARCHAR(6) PK, first_name, last_name, dob, created_at)
providers  (npi VARCHAR(10) PK, name, created_at)
orders     (id PK, patient_mrn FK, provider_npi FK, medication_name,
            primary_diagnosis, order_date, created_at)
care_plans (id PK, order_id FK, content TEXT, status, generated_at)
diagnoses  (id PK, order_id FK, icd10_code)
med_history(id PK, order_id FK, medication_entry)
```

## Validation Rules — Must Always Enforce
| Field | Rule |
|-------|------|
| MRN | Exactly 6 digits |
| NPI | Exactly 10 digits |
| ICD-10 | Regex: `^[A-Z][0-9]{2}(\.[0-9A-Z]{1,4})?$` |
| Provider | If NPI exists in DB, name must match exactly |
| DOB | Valid past date |
| Required fields | first_name, last_name, mrn, dob, provider name, npi, primary_diagnosis, medication_name |

## Duplicate Detection Rules — Must Always Enforce
| Scenario | Response |
|----------|----------|
| Same patient + same medication + same day | ❌ ERROR (HTTP 409) — block |
| Same patient + same medication + different day | ⚠️ WARNING (HTTP 200 + warnings[]) — allow if confirm=true |
| Same MRN + different name or DOB | ⚠️ WARNING — allow if confirm=true |
| Same name + same DOB + different MRN | ⚠️ WARNING — allow if confirm=true |
| Same NPI + different provider name | ❌ ERROR (HTTP 409) — block |

## API Error Response Format — Always Use This Shape
```json
{
  "type": "ERROR | WARNING | VALIDATION",
  "code": "DUPLICATE_ORDER | DUPLICATE_NPI | INVALID_MRN | ...",
  "message": "Human-readable message for the UI",
  "detail": {}
}
```
- Never expose stack traces to the frontend
- Never log PHI (patient names, MRN, DOB) to console

## Care Plan Output — Must Always Include These 4 Sections
1. Problem List / Drug Therapy Problems
2. Goals (SMART format)
3. Pharmacist Interventions / Plan
4. Monitoring Plan & Lab Schedule

## Architecture Rules
- **Controller:** Only reads request, calls service, returns response. No if/else business logic.
- **Service:** All business logic lives here. No direct HTTP concepts (no HttpServletRequest).
- **Repository:** Only Spring Data JPA interfaces. No custom SQL unless necessary.
- **DTO:** Separate classes for request and response. Never expose @Entity directly to frontend.

## Coding Conventions
- Use `@RestControllerAdvice` for global exception handling
- Use `ResponseEntity<?>` as return type in controllers
- Validation annotations on DTOs: `@NotNull`, `@Pattern`, `@Size`
- Service methods throw custom exceptions; controller advice catches them
- Every public service method should have a corresponding unit test

## What NOT to Do
- Do NOT add features beyond what the current Day requires
- Do NOT add WebSocket, SSE, or async before Day 4
- Do NOT add security/auth — out of scope for v1
- Do NOT expose entity classes directly in API responses
- Do NOT put business logic in controllers
- Do NOT log patient data (HIPAA)

## Day-by-Day Progress
- [x] Day 1: Project init, pom.xml, .gitignore, design doc, GitHub
- [ ] Day 2: MVP — POST /api/orders → save to DB → call LLM → return care plan
- [ ] Day 3: Database design — split into Patient, Provider, Order, CarePlan tables
- [ ] Day 4: Async — Redis queue, stop blocking on LLM call
- [ ] Day 5: Worker — consume queue, process LLM in background
- [ ] Day 6: Polling — frontend auto-checks status
- [ ] Day 7: Refactor — Controller / Service / Repository layers
- [ ] Day 8: Validation + duplicate detection + tests
- [ ] Day 9-10: Adapter pattern — handle multiple data sources
- [ ] Day 11: Monitoring — Prometheus + Grafana
- [ ] Day 12-15: AWS deployment + Terraform

## Environment Variables (never commit these)
```
DB_URL=jdbc:postgresql://localhost:5432/careplan
DB_USERNAME=postgres
DB_PASSWORD=your_password
CLAUDE_API_KEY=sk-ant-...
LLM_MODEL=claude-sonnet-4-20250514
```
