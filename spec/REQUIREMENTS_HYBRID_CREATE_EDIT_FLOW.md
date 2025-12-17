# Knights & Kings — Requirements: Hybrid Create/Edit Flow (Web App ⇄ Minecraft ⇄ Web API)

**Document ID:** KKNK-REQ-HYBRID-FLOW  
**Status:** Draft (living document)  
**Scope:** Hybrid create/edit workflow for KnK entities where *business/data* is handled via Web App + Web API and *Minecraft-world dependent* data is captured/validated inside the Minecraft plugin and persisted through the Web API.

---

## 1. Context and Problem Statement

Historically, the legacy Knights & Kings Minecraft plugin handled full entity creation (e.g., Town, District, Street, Structure, Regions) directly inside Minecraft, including persistence via Hibernate/MySQL.

In the new ecosystem, **the Web API is the system of record** and **the Web App is the primary admin UI**. However, certain entity properties can only be determined inside Minecraft (e.g., WorldGuard region identifiers, world locations, selections). Therefore, entity creation/editing must be a **hybrid** workflow spanning:

- **Web App**: primary data entry, orchestration, admin UX
- **Web API**: persistence, validation, workflow state, audit/diagnostics, task coordination
- **Minecraft Plugin**: world-bound actions (WorldGuard/WorldEdit/location capture), in-game guidance, returning results to the API

---

## 2. Goals

1. **Hybrid Create & Edit**: Support creating and editing KnK entities where some steps require Minecraft-world interaction.
2. **API-First Persistence**: Web API remains the authoritative store for entities and workflow state.
3. **Non-Blocking Gameplay**: Plugin must never perform blocking I/O on the main thread; all API calls are async.
4. **Administrator UX**: Admins can complete entity workflows by switching between Web App and Minecraft using a guided, resumable process.
5. **Draft/Resumable**: Partial progress is persisted; a failure does not force redoing complex Minecraft selections/regions.
6. **Auditability & Diagnostics**: API and Web App provide visibility into in-progress steps, task assignment, failures, and results.

---

## 3. Non-Goals (for initial implementation)

- Player-facing Web App flows (future scope).
- Fully automated world edits beyond the specific step being requested (e.g., mass region generation).
- Real-time collaborative editing between multiple admins (can be added later).
- Full state reconciliation if external plugins manually change WorldGuard regions (only detect/report in v1).

---

## 4. Actors and Roles

- **Admin (Web App user)**: Initiates and completes create/edit workflows.
- **Minecraft Admin (in-game)**: Same identity as Admin, authenticated/linked to Web App account (see Identity section).
- **Web API**: Orchestrates workflow, validates inputs, persists entities and tasks.
- **Minecraft Plugin Client**: Performs world-bound steps; can be one or multiple Minecraft servers/instances.
- **Web App (Admin UI)**: Wizard-style UX, showing steps and status.

---

## 5. Key Concepts

### 5.1 Hybrid Workflow Session (Wizard)
A create/edit flow is modeled as a **multi-step workflow**. Steps can be:

- **Web-only step**: field entry/validation; no Minecraft dependency.
- **World-bound step**: requires Minecraft plugin involvement and produces/validates Minecraft-specific outputs.

### 5.2 Draft Entity / Pending State
During create (and sometimes edit), the entity exists in a **draft/pending** state in the API until required world-bound steps are completed.

### 5.3 World Task (World-bound Work Item)
A world-bound step is represented by a **task** assigned to (or claimable by) a Minecraft plugin client. Examples:
- Create or link a WorldGuard region
- Capture a location (spawn/center/etc.)
- Validate region shape/constraints

Tasks must be trackable, retryable, and fail-safe.

### 5.4 Identity Linking (Admin ⇄ Minecraft)
The system must support mapping a Web App admin to an in-game identity to allow "continue your flow in Minecraft". A coupling mechanism may be:
- short-lived **link code**
- logged-in session token (if plugin can authenticate)
- explicit "claim task" by in-game user

**Requirement:** The design must allow secure association without exposing secrets in chat/logs.

---

## 6. Workflow States

### 6.1 Entity lifecycle states (example)
- `Draft` / `PendingWorldBinding` — entity created but missing world-bound data
- `Active` — fully created and usable
- `EditPending` — edit started but missing required world-bound updates
- `Archived/Deleted` — if applicable

### 6.2 Task lifecycle states
- `Pending` — created by API, awaiting claim
- `InProgress` — claimed by a plugin client (optionally with a lease/timeout)
- `Completed` — result submitted and accepted
- `Failed` — result could not be produced (error captured, can be retried)
- `Cancelled` — workflow aborted

---

## 7. Functional Requirements

### 7.1 Web App Requirements (Admin Wizard)

**WA-01 — Wizard-based create/edit**
- Provide step-by-step create/edit for supported entities (Town first; others later).
- Persist step progress via API; refresh-safe and resumable.

**WA-02 — World-bound step UX**
- For world-bound steps, present clear instructions:
  - what to do in Minecraft
  - how to use link code / how to claim task
  - current status (Pending/InProgress/Completed/Failed)
- Show task output once completed (e.g., region name/id, location coordinates).

**WA-03 — Choose existing vs create new**
- For fields like a WorldGuard/WorldCard region:
  - Option A: select existing region (from API list)
  - Option B: create new region via Minecraft step
- Similar pattern for world locations.

**WA-04 — Retry & recovery**
- If a world-bound step fails, allow retry without losing already-completed steps.
- Display meaningful failure reasons and suggested actions.

**WA-05 — Admin diagnostics**
- Provide a “workflow status” panel:
  - entity state
  - tasks list
  - claimed by which server/client and when
  - error messages and history

### 7.2 Web API Requirements (Orchestration & Source of Truth)

**API-01 — Draft create**
- Support creating a draft entity with business fields.
- Persist immediately; return entity id and workflow state.

**API-02 — World task creation**
- For each world-bound step, create an associated task:
  - correlated to entity id and step id
  - includes required input parameters (e.g., desired region type)
  - supports claim/complete/fail

**API-03 — Validation**
- Perform all cross-client business validation in the API:
  - unique names (where applicable)
  - referential integrity
  - permission checks
  - invariant constraints that do not require Minecraft world inspection

**API-04 — Finalization**
- Once all required tasks complete, allow finalization:
  - update entity with world-bound outputs (e.g., `WgRegionId`, `LocationId` or contract-accurate equivalents)
  - transition entity state to `Active`

**API-05 — Edit workflow**
- Support edit workflows similarly:
  - start edit session
  - update business fields
  - create tasks for world-bound changes when needed
  - finalize edit to apply changes atomically

**API-06 — Idempotency**
- Task completion endpoints must be idempotent:
  - duplicate submissions should not corrupt state
  - allow safe retry from plugin after transient errors

**API-07 — Concurrency control**
- Prevent conflicting edits:
  - optimistic concurrency (ETag/row version) or workflow-level locking.
- One active create/edit workflow per entity (configurable).

**API-08 — Audit trail**
- Record:
  - who initiated create/edit
  - step transitions
  - task lifecycle events
  - world-bound outputs and approvals

**API-09 — Contract-first**
- Swagger/OpenAPI is the primary contract for clients.
- Any new fields and endpoints must be reflected in swagger and used by plugin/web app generation.

### 7.3 Minecraft Plugin Requirements (World-bound Execution)

**PL-01 — Task intake**
- Plugin can list/claim tasks relevant to it.
- The plugin identifies itself to API as a distinct client (server id/instance id).

**PL-02 — Guided in-game flows**
- For a claimed task, the plugin guides the admin:
  - prompts and confirmations
  - providing tools (e.g., WorldEdit wand) if appropriate
  - validations (selection present, shape type, constraints)

**PL-03 — WorldGuard/WorldCard region creation/linking**
- For tasks requiring regions:
  - create region based on selection
  - or validate/link an existing region
- Return a stable identifier to API (e.g., region id/name/UUID depending on strategy).

**PL-04 — Location capture**
- For tasks requiring locations:
  - capture player position or selection-derived point
- Return a stable identifier (`LocationId`) and/or details as required by the API contract.

**PL-05 — Async networking**
- All API calls async (CompletableFuture + executor).
- Any Bukkit/Paper world changes are performed on the main thread.

**PL-06 — Resume and retry**
- If plugin crashes/restarts, tasks can be re-listed and resumed/failed gracefully.
- Re-claim behavior should be safe under leases/timeouts.

**PL-07 — Commands (minimum)**
- `/knk tasks list`
- `/knk task claim <id>` (optional if automatic claim exists)
- `/knk task status <id>`
- `/knk health` (already present)

---

## 8. Data Requirements (example: Town)

> Field names below are examples and must match the current swagger contract in implementation.

**Business fields (Web App/API)**
- Name, Description
- Ownership / role assignments (admin-defined)
- Relations (districts/structures, etc.)

**World-bound fields (Plugin-produced)**
- `WgRegionId` (WorldGuard/WorldCard region reference)
- `LocationId` and/or Location payload (world + x/y/z + yaw/pitch, depending on API design)

---

## 9. Security Requirements

**SEC-01 — Auth & authorization**
- Web App uses admin auth.
- Plugin uses service auth (API key / bearer).
- Task claim/complete must enforce authorization and proper correlation to admin identity (as designed).

**SEC-02 — Secret handling**
- Never log auth tokens or API keys in plugin logs.
- Link codes must be short-lived and single-use (recommended).

**SEC-03 — Least privilege**
- Plugin credentials should be scoped to world-task operations and required reads.

---

## 10. Reliability & Error Handling

**REL-01 — Fail-safe partial progress**
- Completed steps remain saved if later steps fail.

**REL-02 — Clear failure modes**
- Task failure records: reason + actionable message.
- Web App shows failure and allows retry.

**REL-03 — Timeouts and retries**
- API client applies reasonable timeouts and limited retries for idempotent calls.
- Task lease expiration should allow re-claim.

---

## 11. Observability

**OBS-01 — Workflow visibility**
- Web App can show live-ish status (polling is acceptable initially).

**OBS-02 — Structured logging**
- API logs include correlation ids (entity id, task id).
- Plugin logs include task id and endpoint.

**OBS-03 — Metrics (optional v1)**
- Count tasks by status, avg completion time, failure rate.

---

## 12. UX Requirements (Admin)

- The Web App wizard must clearly indicate when Minecraft input is required.
- Provide a “copy code” button and simple in-game instruction text.
- Provide a “Verify completion” / refresh action that polls status.
- Avoid forcing admins to reselect regions if steps later fail.

---

## 13. Testing & Acceptance Criteria

### 13.1 Minimum acceptance (v1 for Town)
- Web App can start Town create with business fields -> API stores Draft/Pending state.
- Web App can trigger a world-bound step and show a link code / instruction.
- Plugin can claim the relevant task and complete it by producing `WgRegionId` and `LocationId` (or contract-accurate equivalents).
- API finalizes Town -> state Active.
- Web App reflects final state; audit trail shows step/task history.
- No blocking I/O on Paper main thread.

### 13.2 Regression checks
- Restart plugin mid-task -> task remains recoverable.
- Duplicate completion request -> idempotent behavior.
- Unauthorized claim/complete -> rejected and logged.

---

## 14. Open Questions (to refine next)

1. Exact task claim model: automatic polling vs explicit claim by admin?
2. Link code format/lifetime and how admin identity is verified in-game.
3. Exact WorldGuard integration strategy: region name vs UUID vs custom id.
4. Location schema: store full coordinates vs reference entity only?
5. How edits differ from creates: do edits require staging or immediate application?
6. Do we support multiple Minecraft servers/worlds simultaneously? How is target world selected?
7. Desired UX for region selection: cuboid vs polygon vs custom shapes support.

---

## 15. Definition of Done (per entity enablement)

For each entity (Town, District, Street, Structure, …), the feature is “Done” when:
- Spec + reconcile are complete and consistent with swagger contracts.
- Web App has wizard steps and can complete a full create with required world-bound steps.
- Web API supports draft/workflow/task/finalize for that entity.
- Plugin supports task execution and returns correct outputs.
- End-to-end test on dev server succeeds and is documented.

---

## 16. Generic Requirement: Region Containment Validation (WorldCard/WorldGuard Regions)

### 16.1 Summary

For any entity that uses a WorldCard/WorldGuard region, the system must support **configurable containment validation**: a region for one entity must be fully contained within a specified “parent” region (belonging to another entity).

This rule must be **configurable by administrators** (not hard-coded to Town/District only) and must apply to both **create** and **edit** flows when configured.

### 16.2 Administrator-configurable rules

**REG-01 — Configurable containment rule**
- An administrator can configure, per entity type (and/or per workflow step), whether containment validation is required.

**REG-02 — Select parent region source**
- An administrator can specify which region is the “parent” region to validate against.
- The rule must support scenarios where the parent region comes from a related entity (e.g., “District region must be within its owning Town region”), but must not be limited to that example.

**REG-03 — Multiple containment checks**
- The system must support multiple containment validations for the same workflow if needed (e.g., validate against multiple constraints, or nested parents).

### 16.3 Runtime validation behavior (Plugin)

**REG-04 — Immediate feedback**
- During in-game region definition (selection/editing), the plugin must validate containment and provide immediate/near-immediate feedback when any part lies outside the configured parent region.

**REG-05 — Blocking invalid completion**
- The plugin must prevent completion of the region-definition task if containment validation fails.

**REG-06 — Error messaging**
- Error feedback must clearly indicate that some selected points/edges are outside the parent region and instruct the admin to adjust the selection.
- Messaging must be generic and not tied to specific entity names.

### 16.4 Acceptance criteria

- It is impossible to finalize a world-bound region task when configured containment rules are violated.
- Admins can configure which containment checks apply and which parent region(s) are used.
- The same mechanism applies to any entity that uses regions (not only Town/District).
