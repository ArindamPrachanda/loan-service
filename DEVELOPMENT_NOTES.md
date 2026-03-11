# Development Notes — Loan Application Service

## Overall Approach
[Spring Initializr](https://start.spring.io/) has been used for initial Project setup.

Built as a layered Spring Boot REST service following a clean architecture:
- **Controller** — HTTP handling, request/response mapping
- **Service** — Business logic (eligibility, EMI, risk, offer generation)
- **Domain** — Entities, enums, and intermediate models
- **Repository** — JPA persistence layer
- **Exception** - Global Exception Handling

The orchestration flow in `LoanApplicationService` follows a clear linear pipeline:
1. Classify risk band
2. Calculate interest rate
3. Calculate EMI
4. Run eligibility checks
5. Generate offer (with 50% income check)
6. Persist and return response

## Key Design Decisions

### Single Responsibility per Method
Each concern is isolated into its own service class:
- `RiskBandChecking` — credit score → risk band mapping
- `InterestRateCalculation` — premium calculation logic
- `EmiCalculator` — pure financial math
- `EligibilityChecking` — rule evaluation
- `OfferGeneration` — offer feasibility check
- `LoanApplicationService` — Having all the methods having all business logics

### BigDecimal Throughout
All financial calculations use `BigDecimal` with `scale=2` and `RoundingMode.HALF_UP` as specified,
preventing floating-point precision issues.

### Eligibility vs Offer Check — Two Separate Gates
- **Eligibility** (60% EMI check) is a hard rejection rule
- **Offer generation** (50% EMI check) is the offer viability check
Both are evaluated separately to produce the correct rejection reasons.

### Audit Storage
Every application — approved or rejected — is persisted to the database with its full
decision context (risk band, interest rate, EMI, rejection reasons) for auditability.

## Trade-offs Considered
- `H2 vs real DB` — H2 in-memory for simplicity; easily swappable via `application.properties`

## Assumptions Made
- EMI > 60% is the eligibility rejection gate; EMI > 50% is the offer rejection gate.
- Interest rate is stored as annual percentage (e.g., 13.5 means 13.5% p.a.)
- `totalPayable = EMI × tenureMonths` (does not subtract principal for simplicity)

## Improvements With More Time

- Add `GET /applications/{id}` endpoint to retrieve a stored decision
- Replace H2 with PostgreSQL/OracleDB for production readiness
- Add pagination for any list endpoints
- Add Spring Security / API key authentication
- Add structured logging with correlation IDs
- Externalize constants (base rate, thresholds) to `application.properties` for configurability
- Add Actuator endpoints for health and metrics
