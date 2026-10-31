# Cynbit Technical Assessment — Reference Implementation

This repository contains the backend assessment deliverable for Cynbit Technologies:
* **`AUDIT.md`**: Detailed technical findings across edge compute, security headers, form validation, and SEO redirects.
* **`REPORT.md`**: Executive summary, methodology, and submission notes.
* **`remediation/`**: Drop-in Next.js (`next.config.mjs`) and Vercel (`vercel.json`) configurations.
* **`spring-boot-reference/`**: Java 21 / Spring Boot 3 reference backend contrasting the observed Server Action with a hardened, validated REST API.

---

## Prerequisites

* **Java**: JDK 21 (or JDK 17)
* **Build Tool**: Maven 3.9+

---

## Running the Automated Tests

The test suite runs 4 integration tests verifying the observed baseline (`200 OK`), hardened inquiry creation (`201 Created`), field-level validation errors (`422 Unprocessable Entity`), and request deduplication via `Idempotency-Key`.

From the project root:

```bash
cd spring-boot-reference
mvn clean test
```

Expected output:
```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Running the Application Locally

Start the Spring Boot server:

```bash
cd spring-boot-reference
mvn spring-boot:run
```

The application will start on port `8080`.

---

## API Testing with `curl`

Once the server is running, open a new terminal tab to test the endpoints:

### 1. Observed Baseline Endpoint (Simulating Current Production)
```bash
curl -i -X POST http://localhost:8080/contact-observed \
  -H "Content-Type: application/json" \
  -d '{}'
```
*Returns `200 OK` with `{"success":true}`.*

---

### 2. Hardened Endpoint — Field Validation Error (HTTP 422)
Tests boundary enforcement on invalid email, short name, and short message:
```bash
curl -i -X POST http://localhost:8080/api/v1/inquiries \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "A",
    "email": "invalid-email",
    "message": "Too short"
  }'
```
*Returns `422 Unprocessable Entity` with a structured field-error map.*

---

### 3. Hardened Endpoint — Valid Submission & Security Headers (HTTP 201)
```bash
curl -i -X POST http://localhost:8080/api/v1/inquiries \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-101" \
  -d '{
    "fullName": "Alex Rivera",
    "email": "alex@example.com",
    "company": "Acme Corp",
    "industry": "Fintech",
    "message": "We need an autonomous AI workflow for document processing."
  }'
```
*Returns `201 Created` with an assigned `id`, UTC timestamp, and defensive security headers (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`).*

---

### 4. Idempotency Check (Duplicate Request Protection)
Run the exact same command above a second time with the same `Idempotency-Key: test-key-101`.

*Returns `200 OK` with `"idempotentReplay": true` and the original receipt, ensuring duplicate leads are never written to storage on network retries.*