---
name: domain-driven-design-review
description: Use this skill when asked to review the codebase for Domain-Driven Design quality, find DDD violations, check for anemic domain models, check for layer bleed-through, audit repositories, or produce DDD improvement recommendations. Trigger on phrases like "DDD review", "domain model review", "check architecture", "review for DDD", "find layer violations", "anemic model check", "are my repositories clean".
version: 1.0.0
---

# Domain-Driven Design Review

You are acting as a Senior Software Architect with deep expertise in Domain-Driven Design (DDD). Your job is to audit this codebase, diagnose where DDD principles are being violated, and produce a prioritized list of the **top 10 most impactful improvements**.

Be direct, specific, and surgical. Reference actual class names, method names, and line numbers. Explain *why* each finding is a DDD violation — not just that it is — so the developer understands the principle, not just the fix.

---

## Codebase Overview

Base package: `com.alwaysmoveforward.configurationmanager`
All source under: `backend/src/main/java/com/alwaysmoveforward/configurationmanager/`

Layers (inside-out):
- `domainmodel/` — DDD aggregates, entities, value objects
- `data/` — repositories, DAOs (`data/dao/`), entities (`data/Entities/`), mappers (`data/mapper/`)
- `services/` — application/domain services
- `web/` — REST controllers (`web/API/`), view models (`web/Models/`)
- `security/` — cross-cutting auth infrastructure

---

## How to Conduct the Review

### Step 1: Read the domain models

Read every file in `domainmodel/`. For each class, ask:
- Does it contain business logic, or is it just getters/setters? (anemic model test)
- Does it import from `data/` or `web/`? (layer violation)
- Are state transitions enforced by the object itself, or managed externally by services?
- Are there invariants (rules that must always hold) that the class enforces, or does it rely on callers to be careful?

### Step 2: Audit the repositories

Read every file in `data/repositories/`. For each class, ask:
- Do any method signatures use Entity types (`data/Entities/`) as parameters or return values? (layer leak)
- Does the repository call DAOs from other repositories directly? (cross-repo DAO calls)
- Is there complex business logic inside `save()` or query methods that belongs in the domain or a service?
- Are there raw SQL strings built by concatenation? (fragile query construction, possible SQL injection)

### Step 3: Audit the services

Read every file in `services/`. For each class, ask:
- Does it import Entity types directly from `data/Entities/`? (layer violation)
- Is there logic here that clearly belongs on a domain model (e.g., state transitions, invariant checking)?
- Is authorization/permission checking mixed in with business logic?

### Step 4: Audit the controllers

Read every file in `web/API/`. For each class, ask:
- Is there business logic (conditionals, calculations, searches) in controller methods?
- Does the controller call repositories directly instead of going through services?
- Does the controller import from `data/`?

### Step 5: Check for value objects

Look at constants and small "carrier" classes across all layers. Ask:
- Are there string or integer constants that represent domain concepts (states, permissions, roles)? These should be enums or value objects.
- Are there primitive-obsession patterns (e.g., passing `userId` as a `long` everywhere rather than a typed `UserId`)?

---

## DDD Principles to Evaluate Against

### Anemic Domain Model (the most common violation)
A domain model is anemic when its classes are just bags of getters/setters, with all real logic living in services. In a rich domain model, the objects enforce their own invariants, manage their own state transitions, and expose intent-revealing operations. The test: can you read the domain model and understand the business rules without looking at the services?

**What to look for:** Methods named `setX()` / `getX()` covering 90% of the class. State changes applied from outside (`item.setStatus(STATUS_PUBLISHED)` in a service). Validation logic that lives in a service rather than in the object being validated.

### Layer Bleed-Through
Each layer should only depend on layers below it, and only through the interfaces those layers expose. The data layer exposes domain models (not entities) to the layers above it. The domain layer knows nothing about persistence or HTTP.

**What to look for:** `import data.Entities.*` anywhere outside the `data/` package. `import web.*` anywhere in `services/` or `domainmodel/`. Controllers calling DAOs or repositories directly. Services constructing JPA queries.

### Repositories Not Hiding Out-of-Process Details
A repository's contract is to make persistence invisible. Callers should hand it domain objects and get domain objects back — they should have no idea whether the backing store is a database, an API, or an in-memory map.

**What to look for:** Method signatures with Entity types as parameters or return values. Repositories that call other repositories' DAOs directly (cross-DAO coupling). Repository `save()` methods that contain long diff-and-sync logic that belongs in the aggregate. Raw SQL strings built in the repository.

### Scattered Authorization / Missing Domain Policy Objects
Authorization rules are part of the domain. When permission checks are scattered across controllers, services, and repositories, the rules are hard to reason about and easy to miss.

**What to look for:** `if (user.hasRole(...))` checks in controllers or service methods. Inline string comparisons against role/permission names instead of typed policy objects.

### Missing or Incomplete Aggregate Boundaries
An aggregate is a cluster of domain objects with a single root that enforces consistency. All modifications to objects within the aggregate must go through the aggregate root. This is what makes `save()` reasoning tractable.

**What to look for:** Sub-objects (e.g., Contact, ContactChannel) being modified and saved independently via their own repositories, bypassing the aggregate root's invariants. Repositories for things that should only exist as part of a larger aggregate.

---

## Known Patterns in This Codebase

Use this section to ground your review — these are real patterns observed here that the review should consider:

---

## Output Format

Produce exactly **10 recommendations**, ranked by impact on DDD quality. Use this structure for each:

```
### #N — [Short title]

**DDD Principle Violated:** [which principle]
**Severity:** Critical | High | Medium

**What's wrong:**
[Concrete description of the violation, naming the specific class/method/line]

**Why it matters:**
[The consequence — what goes wrong in the system as the codebase grows]

**Recommended fix:**
[Specific, actionable change — what to move, what to introduce, what to remove]
```

After the 10 items, add a short **Patterns to Address Systemically** section that calls out themes spanning multiple items (e.g., "authorization is scattered in 6 places — consider a single policy layer").

---

## Tone and Depth

- Be specific: name the file, the method, the line range where possible.
- Explain the "why" behind each DDD principle — do not just say "this violates DDD".
- Prioritize by impact: a finding that corrupts aggregate consistency is more critical than one that just violates code style.
- Do not pad the list. If there are fewer than 10 distinct issues of real substance, say so and report what exists.
- Do not recommend things that are already done correctly.
