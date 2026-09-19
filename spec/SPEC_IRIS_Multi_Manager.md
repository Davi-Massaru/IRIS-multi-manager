# IRIS Multi-Manager — Product & Technical Specification

**Status:** MVP Specification  
**Contest:** InterSystems Programming Contest — Build Your Own Management Portal  
**Primary stack:** Angular + Quarkus + InterSystems SysAdmin APIs  
**MVP topology:** 3 independent InterSystems IRIS Community Edition instances

---

## 1. Executive Summary

IRIS Multi-Manager is a web-based, external administration console for managing multiple independent InterSystems IRIS instances from a single workspace.

The product is intentionally **fleet-first**. It does not ask the administrator to select one IRIS instance and then reproduce the traditional Management Portal. Instead, every resource is shown together with the instance where it exists, and administrative operations can be validated and applied to multiple selected instances.

The flagship capability is the **Global Process Explorer**: the administrator searches processes across all connected IRIS instances and immediately sees where each process is running. A process is therefore identified by the composite identity `(instance, PID)`, not only by PID.

The same multi-instance model is applied to the contest areas:

- Web Applications and REST applications
- Permissions
- Security, Wallet, X509 and OAuth
- Tasks
- Processes and system resources
- Logs/audit information

The application consumes the official SysAdmin OpenAPI specification published for the contest:

- https://github.com/intersystems-community/sysadmin-api-specification
- https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json

The official specification declares OpenAPI 3.0, API version 2 and `/api/admin` as its base URL.

---

## 2. Problem

Large IRIS environments commonly contain multiple independent instances for development, QA, production, batch processing, reporting, integration or other workloads.

The traditional single-instance mental model creates recurring operational friction:

1. An administrator knows that a process is running, but does not know **which IRIS instance** is running it.
2. The same Web Application, Task, Role, OAuth configuration or other administrative resource should exist on multiple instances, but there is no immediate fleet-wide view showing where it exists and where it does not.
3. A configuration change must be repeated manually on several instances.
4. Repeating the same operation manually can create configuration drift or partial changes.
5. Errors and operational data are fragmented by instance, forcing administrators to inspect multiple portals.

The central problem is therefore not lack of administrative APIs. The APIs exist per instance. The missing layer is **multi-instance orchestration and visibility**.

---

## 3. Product Thesis

> **Manage the IRIS environment, not one IRIS server at a time.**

The main user questions are:

- **WHERE?** — On which instance is this process/resource?
- **PRESENT?** — On which instances does this resource exist?
- **DIFFERENT?** — Is the resource configured differently across instances?
- **APPLY?** — Can I safely apply this change to selected instances?
- **RESULT?** — On which instances did the operation succeed, fail or get skipped?

---

## 4. Goals

### 4.1 Primary goals

- Connect to multiple independent IRIS instances.
- Use the official SysAdmin APIs as the primary management integration.
- Provide a unified Global Process Explorer.
- Always show the owning IRIS instance for runtime resources.
- Provide a reusable Resource Presence Matrix.
- Compare resource configuration across instances.
- Apply supported administrative changes to multiple selected instances.
- Perform pre-flight validation before multi-instance mutations.
- Preserve per-instance results instead of hiding partial failures.
- Support IRIS Community Edition.

### 4.2 Community Opportunity goal

Implement **“Querying from multiple IRIS instances”** as a real feature, not only as inspiration.

A dedicated **Fleet Query** utility will execute a read-only SQL query against multiple selected IRIS connections and return results separated by instance. This feature uses the official InterSystems JDBC driver and is intentionally secondary to the SysAdmin management experience.

The idea is listed by InterSystems as a Community Opportunity in *InterSystems Ideas News #15* and is still surfaced by Open Exchange as waiting to be implemented.

### 4.3 Non-goals for MVP

The MVP will not implement:

- ECP management or ECP-dependent functionality
- schema synchronization
- performance/selectivity drift analysis
- JMeter/load testing
- AI, LLM, RAG or autonomous agents
- automatic production remediation
- distributed transactions across IRIS instances
- a full replacement for every Management Portal page

---

## 5. Target User

Primary persona: **IRIS DBA / system administrator / platform administrator** responsible for more than one IRIS instance.

Typical environment:

```text
DEV-01
QA-01
PROD-01
PROD-02
BATCH-01
REPORT-01
...
```

The MVP demo uses only three independent instances, but the architecture must not encode a three-instance limit.

---

# PART I — PRODUCT SPECIFICATION

## 6. Connection Management

### 6.1 Instance model

Each connection contains at minimum:

```text
id
name
adminBaseUrl
environment/tag
username
authMode
jdbcHost (optional, required by Fleet Query)
jdbcPort (optional)
defaultNamespace (optional)
enabled
```

Example:

```text
Name: PROD-01
Admin URL: http://iris-prod-01:52773/api/admin
Environment: PRODUCTION
Username: admin
Auth mode: Basic or Bearer
JDBC: iris-prod-01:1972/USER
```

### 6.2 Authentication

The SysAdmin specification supports both Bearer JWT and HTTP Basic authentication. `/login` returns JWT tokens and is documented as available starting with IRIS 2026.2. The MVP client therefore supports:

1. **Basic authentication** as the compatibility baseline.
2. **JWT login** when the target supports `/api/admin/login`.

Passwords must not be written to browser localStorage.

For the contest MVP, credentials may be held only in the authenticated Quarkus server session/in-memory credential context. Persistent encrypted credential storage is a post-MVP feature.

### 6.3 Connection validation

After authentication, the backend calls:

```http
GET /api/admin/info
```

This validates connectivity and returns information about the server, API and authenticated user.

---

## 7. Global Process Explorer — Flagship Feature

### 7.1 User problem

The administrator may know the name/routine/user/namespace of a process but not know which IRIS instance is currently running it.

### 7.2 Fleet-wide process list

The backend calls the process list endpoint against all selected instances:

```http
GET {instance}/api/admin/v2/processes
```

The official API supports a `filter` parameter and returns the process list with `%Admin_Operate:U` permission.

The Multi-Manager normalizes every result into:

```text
instanceId
instanceName
pid
namespace
routine
user
state
cpuTime
clientIPAddress
...
```

The user interface always shows `INSTANCE` as the first-class identifier:

```text
INSTANCE      PID      NAMESPACE      ROUTINE               STATE
PROD-01       4812     APP            QueueProcessor        RUN
PROD-03       9912     APP            QueueProcessor        HANG
QA-01         8121     APP            BatchProcessor        RUN
```

### 7.3 Search

Search can be executed against:

- PID
- routine/process text
- namespace
- user
- state
- client information exposed by the API

Example:

```text
Search: QueueProcessor

FOUND ON 2 INSTANCES
PROD-01 / PID 4812
PROD-03 / PID 9912
```

### 7.4 Process detail

When the user selects a process, the backend calls:

```http
GET {instance}/api/admin/v2/process?pid={pid}
```

The SysAdmin schema exposes detailed process information including namespace, current line/routine, current source line, last global reference, CPU time, memory usage, parent PID, client IP, open devices and other runtime data.

### 7.5 Process actions

Supported operations:

```http
POST /v2/process/suspend
POST /v2/process/resume
POST /v2/process/terminate
```

Safety requirements:

- the target instance name is displayed prominently;
- terminate requires explicit confirmation;
- the action applies only to the selected `(instance, PID)` pair;
- the backend performs a fresh lookup before destructive action to reduce stale-target risk;
- result is returned with the target instance name.

### 7.6 Acceptance criteria

- A single search queries all three MVP IRIS instances.
- Results identify the owning instance.
- Same PID values on different instances do not collide.
- Detail works after choosing a result.
- Suspend/resume/terminate are routed to the correct instance.
- One unavailable instance does not prevent results from healthy instances.

---

## 8. Resource Presence Matrix

The Resource Presence Matrix is the main reusable UI primitive for configured resources.

Example:

```text
WEB APPLICATION       DEV-01    QA-01    PROD-01
/api/fhir                ✓        ✓          ✓
/api/internal            ✓        ✓          ✕
/debug                   ✓        ✕          ✕
```

The matrix answers:

1. Does this resource exist on the instance?
2. Is it enabled?
3. Does its normalized configuration differ?
4. Can the user copy/apply a configuration to another instance?

The same pattern is reused for Web Apps, Tasks, Roles, Resources, X509 credentials, OAuth configurations and Wallet secret presence where technically appropriate.

---

## 9. Web Applications and REST Applications

### 9.1 SysAdmin API

Primary endpoints:

```http
GET    /v2/web-apps
GET    /v2/web-app?name=...
PUT    /v2/web-app?name=...
DELETE /v2/web-app?name=...
```

The official `Application` schema includes configuration such as `DispatchClass`, `Enabled`, authentication-related properties and REST-specific fields. `DispatchClass` can identify REST dispatch use cases such as `CSP.REST`.

### 9.2 Multi-instance features

- fleet list of web apps;
- resource presence matrix;
- configuration comparison;
- select one instance as source/reference;
- update a common property on selected instances;
- create a missing application using a reviewed source configuration;
- verify after mutation.

### 9.3 REST exploration scope

The SysAdmin specification exposes Web Application configuration but does not guarantee a route-level OpenAPI document for every arbitrary user REST application.

Therefore the MVP will:

- identify REST-oriented Web Applications from available configuration metadata;
- display dispatch/authentication/runtime configuration;
- optionally display an application OpenAPI document only when the target application exposes one through a known/configured URL.

The MVP will not invent REST routes that are not exposed by the target.

---

## 10. Permission Management

### 10.1 SysAdmin API

Primary list endpoints:

```http
GET /v2/security/users
GET /v2/security/roles
GET /v2/security/resources
GET /v2/security/sql-privileges
GET /v2/security/sql-admin-privileges
```

The specification also provides mutation endpoints for security resources and SQL privilege grant/revoke operations.

### 10.2 Multi-instance UX

Example:

```text
ROLE / RESOURCE       DEV-01    QA-01    PROD-01
APP_OPERATOR             ✓        ✓          ✓
APP_AUDIT                ✓        ✓          ✕
```

For permissions, comparison must distinguish:

- resource missing;
- role/user missing;
- permission missing;
- same permission;
- permission value differs.

Mutation is always preflighted because a privilege may reference a role/resource that is absent on one target.

---

## 11. Security and Secrets

### 11.1 Wallet

Relevant endpoints include:

```http
GET/PUT/DELETE /v2/wallet/collection
GET            /v2/wallet/collections
GET            /v2/wallet/secrets
GET/PUT/DELETE /v2/wallet/secret
```

`/v2/wallet/secrets` lists secret names/types in a collection.

Fleet view should therefore focus on **presence and metadata**, not revealing secret values.

Example:

```text
SECRET NAME          DEV-01    QA-01    PROD-01
FHIR_API_KEY            ✓        ✓          ✕
```

A secret must never be copied from one target to another by exposing the secret value in Angular. Creating a secret on multiple instances requires explicit user-provided input handled by Quarkus and immediately discarded after the operation.

### 11.2 X509

Relevant endpoints:

```http
GET /v2/security/x509-credentials
GET/PUT/DELETE /v2/security/x509-credential
GET /v2/security/x509-credential/certificate
```

Multi-instance features:

- presence matrix;
- certificate metadata comparison;
- expiration/subject/serial visibility when returned by the API;
- create/edit on selected instances where supported.

### 11.3 OAuth

The specification includes OAuth2 client, server-definition, resource-server, server/client and secret-related operations under:

```text
/v2/security/oauth2/...
```

MVP scope:

- list configurations across instances;
- indicate missing configurations;
- compare non-secret configuration;
- apply supported non-secret configuration changes;
- require explicit secret input for secret mutation.

---

## 12. Task Management

### 12.1 SysAdmin API

Relevant endpoints include:

```http
GET  /v2/tasks
GET/PUT/DELETE /v2/task
POST /v2/task/run
POST /v2/task/suspend
POST /v2/task/resume
GET  /v2/task/upcoming
```

### 12.2 Multi-instance UX

```text
TASK                    DEV-01    QA-01    PROD-01
Purge Messages             ✓        ✓          ✓
Database Backup            ✕        ✕          ✓
Daily Audit                ✕        ✓          ✕
```

Capabilities:

- find task across fleet;
- compare schedules/configuration;
- run/suspend/resume a task on explicitly selected instance(s);
- create/edit configuration on multiple selected instances with pre-flight validation;
- show per-instance result.

---

## 13. Operating System / Runtime Management

### 13.1 Processes

Processes are the flagship feature described in section 7.

### 13.2 System resources

Relevant endpoints:

```http
GET /v2/monitor/dashboard/main
GET /v2/monitor/dashboard/system-resources
GET /v2/monitor/system-usage
GET /v2/monitor/system-usage/shared-memory
```

The fleet view normalizes each instance into a row/card so the administrator can compare current system health without opening each portal.

Example:

```text
INSTANCE      CPU/LOAD     MEMORY     DISK FREE     PROCESSES
DEV-01        ...          ...        ...           ...
QA-01         ...          ...        ...           ...
PROD-01       ...          ...        ...           ...
```

### 13.3 Devices

The specification includes device management under `/v2/device`, including create/edit/delete, device settings and device subtype operations.

The MVP should prioritize read/list/compare and only expose mutation when it provides clear value in the demonstration.

---

## 14. Unified Logs / Events

The provided specification does not expose a single generic `/logs` endpoint. The MVP must therefore aggregate the documented sources rather than scrape IRIS files from the operating system.

The initial unified event view will combine, where available:

1. **Security audit records** — `/v2/security/audit/records` and related audit endpoints.
2. **Task execution information/history/console** exposed by task endpoints.
3. **Dashboard alert/application-error counts** from monitor/dashboard data.
4. Optional journal views remain an advanced administrative area and are not presented as application logs.

Every log/event row must include:

```text
instance
source/subsystem
timestamp
severity/type when available
user when available
message/details
```

Example:

```text
TIME        INSTANCE    SOURCE       TYPE        DETAILS
14:31:02    PROD-01     SECURITY     LOGIN       ...
14:30:55    QA-01       TASK         FAILURE     ...
```

If later revisions of the SysAdmin API expose additional subsystem log endpoints, they can be added behind the same `FleetEventProvider` abstraction without changing the UI contract.

---

## 15. Multi-Instance Mutation Protocol

This protocol is mandatory for every fleet-wide write operation.

### Step 1 — Select targets

The user explicitly chooses instances or a predefined instance group.

### Step 2 — Pre-flight

For each instance the backend performs the relevant GET/detail call and returns:

```text
READY
RESOURCE_MISSING
DEPENDENCY_MISSING
FORBIDDEN
OFFLINE
UNSUPPORTED
```

### Step 3 — Preview

The UI shows the exact target instances and the before/after diff when meaningful.

### Step 4 — Explicit confirmation

No multi-instance mutation is executed directly from a table toggle without confirmation.

### Step 5 — Execute

Quarkus performs bounded concurrent calls to selected IRIS instances.

### Step 6 — Verify

For state-based configuration operations, Quarkus reads the target again and confirms the expected state.

### Step 7 — Report partial results

Example:

```text
PROD-01    SUCCESS
PROD-02    SUCCESS
PROD-03    FAILED — 403 %Admin_Secure required
```

### Important technical limitation

There is **no distributed transaction** across independent IRIS instances. A write can succeed on instance A and fail on B.

The product therefore guarantees **visibility, validation and verification**, not global atomicity.

The MVP will not implement automatic rollback of an earlier successful target after another target fails. This avoids creating a false transactional guarantee.

---

## 16. Fleet Query — Community Opportunity Implementation

### 16.1 Purpose

Provide an explicit implementation of the Community Opportunity **“Querying from multiple IRIS instances.”**

### 16.2 Behavior

The user selects one or more configured IRIS instances and enters a SQL query.

MVP restrictions:

- `SELECT` only;
- statement parser/guard rejects DDL and DML;
- per-instance timeout;
- row limit;
- results remain separated by instance rather than blindly merging incompatible schemas.

Example:

```text
SELECT COUNT(*) FROM Demo.Person

INSTANCE    STATUS    RESULT      ELAPSED
DEV-01      OK        100         11 ms
QA-01       OK        1,000       18 ms
PROD-01     OK        50,000      27 ms
```

### 16.3 Connectivity

Fleet Query uses the official InterSystems JDBC driver. InterSystems documents it as a pure Java Type 4 JDBC 4.2 driver, using a connection URL such as:

```text
jdbc:IRIS://host:1972/NAMESPACE
```

This fits naturally in the Quarkus backend and does not require ODBC.

---

## 17. Backend Architecture

### 17.1 Main modules

```text
connection/
  IrisInstance
  InstanceRegistry
  InstanceAuthenticationService

sysadmin/
  SysAdminClient
  SysAdminClientFactory
  dto/

fleet/
  FleetExecutor
  FleetResult
  FleetTargetResult
  PreflightService

process/
  FleetProcessService
  ProcessResource

webapp/
  FleetWebAppService

security/
  FleetSecurityService

wallet/
  FleetWalletService

task/
  FleetTaskService

monitor/
  FleetMonitorService

logs/
  FleetEventService
  FleetEventProvider

query/
  FleetQueryService
  IrisJdbcConnectionFactory
```

### 17.2 SysAdminClient

`SysAdminClient` represents operations against **one** IRIS instance.

It must not contain fleet logic.

### 17.3 FleetExecutor

`FleetExecutor` is the central reusable orchestration primitive.

Conceptually:

```text
execute(targetInstances, operation)
```

Responsibilities:

- concurrency limit;
- request timeout;
- authentication context;
- isolate errors per instance;
- collect normalized results;
- never fail the entire fleet call because one instance is offline.

### 17.4 Reactive execution

Quarkus REST Client + Mutiny is appropriate because most fleet operations are independent HTTP I/O calls. Three or thirty instances can be queried concurrently with bounded concurrency rather than one after another.

---

## 18. Frontend Architecture

Angular is a thin presentation/orchestration client. It never talks directly to an IRIS instance.

```text
Angular
   |
   v
Quarkus API
   |
   +--> IRIS-A SysAdmin API
   +--> IRIS-B SysAdmin API
   +--> IRIS-C SysAdmin API
```

Primary Angular features:

```text
features/
  instances/
  processes/
  web-apps/
  permissions/
  security/
  tasks/
  system/
  logs/
  fleet-query/

shared/
  instance-selector/
  resource-matrix/
  configuration-diff/
  operation-preview/
  fleet-operation-result/
```

Reusable UI components are essential. The same Resource Matrix and operation result patterns should power multiple administrative domains.

---

## 19. API Coverage Matrix

| Contest capability | SysAdmin API used | Fleet behavior |
|---|---|---|
| Instance validation | `GET /info` | Test/identify every configured instance |
| Authentication | Basic auth; `POST /login`, `/refresh`, `/logout` when supported | Independent authentication per target |
| Processes | `GET /v2/processes`, `GET /v2/process` | Aggregate and search across fleet |
| Process control | `/v2/process/suspend`, `/resume`, `/terminate` | Route operation to correct `(instance, PID)` |
| Web Apps | `GET /v2/web-apps`, `GET/PUT/DELETE /v2/web-app` | Presence matrix, compare, multi-edit |
| Permissions | `/v2/security/users`, `/roles`, `/resources`, SQL privilege endpoints | Presence/diff and selected writes |
| Wallet | `/v2/wallet/collection(s)`, `/secret(s)` | Presence/config management without exposing values |
| X509 | `/v2/security/x509-credential(s)` | Presence and certificate metadata comparison |
| OAuth | `/v2/security/oauth2/...` | Presence/config comparison and supported mutations |
| Tasks | `/v2/tasks`, `/v2/task`, `/run`, `/suspend`, `/resume`, `/upcoming` | Fleet task matrix and controlled actions |
| System resources | `/v2/monitor/dashboard/main`, `/system-resources`, `/v2/monitor/system-usage` | One fleet-wide resource view |
| Devices | `/v2/device...` | Multi-instance read/compare; limited mutation |
| Audit/events | `/v2/security/audit/...` | Unified rows with instance attribution |
| SQL fleet query | JDBC, not SysAdmin API | Read-only query across selected instances |

---

## 20. Security Requirements

- Angular never receives stored IRIS passwords.
- Angular never calls IRIS directly.
- Backend logs must redact Authorization headers, passwords, access tokens and secret values.
- Wallet secret values are never displayed in comparison matrices.
- Mutations require explicit target selection.
- Destructive process actions require explicit confirmation.
- All returned runtime resources contain `instanceId` and `instanceName`.
- Backend enforces allowlisted outbound IRIS URLs to reduce SSRF risk.
- TLS certificate validation is enabled by default for external HTTPS targets.
- An “allow insecure/self-signed certificate” option, if added for demo/dev, must be explicit and visibly marked unsafe.
- Multi-instance operation history records metadata and result, never secret values.

---

## 21. Technical Feasibility

### Processes — HIGH

Directly supported by list/detail/action endpoints. The official API includes a server-side `filter` and detailed process runtime data. This is the strongest and least risky module.

### Web Applications — HIGH

List/detail/create/edit/delete are directly exposed. Presence matrix and multi-edit are orchestration problems handled entirely by Quarkus.

### Tasks — HIGH

List, run, suspend, resume and scheduling-related data are explicitly exposed.

### Permissions/Security — HIGH to MEDIUM

The API exposes users, roles, resources and SQL privileges. Complexity is primarily UX and dependency validation, not missing connectivity.

### Wallet/X509/OAuth — MEDIUM

The endpoints exist, but these objects contain sensitive/dependent configuration. MVP should favor list/presence/compare and carefully selected writes rather than attempting to clone every secret-bearing property automatically.

### System resources — HIGH

Monitor/dashboard endpoints support a fleet resource summary. Community Edition is sufficient; ECP is deliberately excluded.

### Logs — MEDIUM

There is no single generic logs endpoint in the specification. The MVP can reliably aggregate documented audit/task/monitor sources, but “all possible IRIS logs” should not be claimed until every required source is identified in the API.

### Fleet Query — HIGH

Java has an official InterSystems Type 4 JDBC driver. Read-only parallel execution is straightforward in Quarkus.

### Multi-instance atomic update — NOT FEASIBLE AS A TRUE TRANSACTION

Independent IRIS instances cannot be treated as one ACID transaction through these APIs. The correct design is preflight + explicit confirmation + per-instance result + verification.

---

## 22. MVP Screens

1. **Instances**
   - list three configured instances
   - connection status
   - login/connect
   - tags/environment

2. **Processes** — flagship
   - all processes from all instances
   - global search
   - instance column
   - process detail
   - suspend/resume/terminate

3. **Web Apps**
   - fleet presence matrix
   - detail comparison
   - multi-instance edit

4. **Permissions/Security**
   - users, roles and resources presence
   - basic comparison

5. **Secrets**
   - wallet collection/secret presence
   - X509 credential presence/details
   - OAuth configuration presence

6. **Tasks**
   - fleet task matrix
   - run/suspend/resume

7. **System**
   - system resource summary per instance

8. **Logs / Events**
   - audit/task/alert aggregation with instance column

9. **Fleet Query**
   - selected instances
   - SELECT-only editor
   - result grouped by instance

---

## 23. MVP Acceptance Criteria

The MVP is considered complete when:

- three independent IRIS Community Edition instances can be registered and reached;
- the backend validates each instance through the SysAdmin API;
- Global Process Explorer returns a unified list from all three instances;
- a process can be located by search without knowing the target instance beforehand;
- process detail includes the owning instance;
- at least one process action is demonstrated against the correct instance;
- Web Apps are rendered as a 3-instance presence matrix;
- one Web App configuration change can be preflighted, applied to two or more selected instances and verified;
- Tasks are rendered across all three instances;
- Users/Roles/Resources have at least a read-only fleet matrix;
- Wallet/X509/OAuth have at least a read-only presence view;
- System resources are shown per instance;
- audit/event information includes instance attribution;
- Fleet Query executes the same SELECT against at least two IRIS instances;
- failure/offline behavior is per-instance and does not collapse the entire page;
- Docker Compose starts the complete demo environment.

---

# PART II — MVP ARCHITECTURE & DOCKER INFRASTRUCTURE

## 24. MVP Architecture

```text
                         BROWSER
                            |
                            v
                  +-------------------+
                  |      ANGULAR      |
                  |  Fleet-first UI   |
                  +---------+---------+
                            |
                      HTTPS / REST
                            |
                            v
                  +-------------------+
                  |      QUARKUS      |
                  |                   |
                  | Instance Registry |
                  | Fleet Executor    |
                  | SysAdmin Clients  |
                  | Fleet Query/JDBC  |
                  +----+---------+----+
                       |         |
          SysAdmin API |         | JDBC (Fleet Query only)
                       |         |
         +-------------+---------+-------------+
         |                       |             |
         v                       v             v
 +---------------+       +---------------+ +---------------+
 |   IRIS DEV    |       |    IRIS QA    | |   IRIS PROD   |
 | Community Ed. |       | Community Ed. | | Community Ed. |
 | /api/admin    |       | /api/admin    | | /api/admin    |
 | JDBC :1972    |       | JDBC :1972    | | JDBC :1972    |
 +---------------+       +---------------+ +---------------+
```

The three IRIS containers are completely independent. They are **not ECP nodes** and do not require ECP.

---

## 25. Docker Services

Recommended MVP services:

```text
frontend
backend
iris-dev
iris-qa
iris-prod
```

No PostgreSQL is required for the first contest MVP.

Connection metadata can be seeded in Quarkus or persisted to a small mounted local store; credentials remain session-scoped. A database can be introduced later for multi-user persistence/audit history.

### Suggested ports

| Service | Container port | Host/demo port |
|---|---:|---:|
| Angular/Nginx | 80 | 8080 |
| Quarkus | 8080 | internal only or 8081 |
| IRIS DEV web | 52773 | 52773 |
| IRIS DEV superserver | 1972 | 1972 |
| IRIS QA web | 52773 | 52774 |
| IRIS QA superserver | 1972 | 1973 |
| IRIS PROD web | 52773 | 52775 |
| IRIS PROD superserver | 1972 | 1974 |

Inside the Docker network, Quarkus uses stable service names and container ports:

```text
http://iris-dev:52773/api/admin
http://iris-qa:52773/api/admin
http://iris-prod:52773/api/admin

jdbc:IRIS://iris-dev:1972/USER
jdbc:IRIS://iris-qa:1972/USER
jdbc:IRIS://iris-prod:1972/USER
```

---

## 26. Recommended Docker Network

```text
iris-multi-manager-net
```

Only the Angular/Nginx entry point must be public for normal use.

For contest/demo purposes, the three IRIS web ports may also be exposed so judges can independently inspect each Management Portal and confirm that Multi-Manager is operating on different instances.

Angular must not connect to the IRIS ports directly.

---

## 27. Frontend Container

Production MVP:

```text
Angular build
    -> static files
    -> nginx
```

Nginx responsibilities:

- serve Angular;
- proxy `/api/*` to Quarkus;
- optionally terminate TLS in deployed online demo.

External access:

```text
http://localhost:8080
```

or the public demo hostname.

---

## 28. Backend Container

Recommended runtime:

```text
Java 21
Quarkus JVM mode
```

Quarkus dependencies:

- Quarkus REST
- Quarkus REST Client
- Mutiny
- Jackson
- Bean Validation
- SmallRye OpenAPI for the Multi-Manager API
- InterSystems JDBC driver for Fleet Query

Native image is not a requirement for MVP. JVM mode lowers integration risk and is sufficient for the contest.

---

## 29. Demo Data / Three-Instance Scenario

Seed the three IRIS containers intentionally with small differences so the multi-instance value is visible.

### DEV

```text
Web Apps:
/api/demo
/api/internal

Tasks:
DemoCleanup

Process:
DemoWorker
```

### QA

```text
Web Apps:
/api/demo
/api/internal

Tasks:
DemoCleanup
DemoAudit

Process:
DemoWorker
```

### PROD

```text
Web Apps:
/api/demo
# /api/internal intentionally missing

Tasks:
DemoCleanup

Process:
DemoWorker or another controlled test job
```

Demo storyline:

1. Open Global Process Explorer and locate `DemoWorker` across all instances.
2. Open one result and show the exact IRIS instance + PID.
3. Open Web Apps and show `/api/internal` missing from PROD.
4. Select a safe configuration property and apply a change to DEV + QA.
5. Show preflight/result per instance.
6. Open Tasks and show `DemoAudit` exists only on QA.
7. Open System view and compare all three containers.
8. Open Logs/Events and show instance attribution.
9. Finish with Fleet Query executing the same SELECT against all three instances.

---

## 30. Repository Layout

```text
iris-multi-manager/

  frontend/
    src/
    Dockerfile

  backend/
    src/main/java/
    src/main/resources/
    pom.xml
    Dockerfile

  docker/
    iris/
      dev/
      qa/
      prod/
      init/

  docker-compose.yml
  SPEC.md
  README.md
```

---

## 31. Suggested Implementation Order

### Phase 1 — vertical slice

- Docker Compose with 3 IRIS instances
- Quarkus Instance Registry
- SysAdmin authentication
- `/info`
- `/v2/processes`
- Angular process table with instance column

### Phase 2 — flagship completion

- global process search
- process detail
- suspend/resume/terminate
- per-instance timeout/error handling

### Phase 3 — resource framework

- generic `FleetExecutor`
- generic Resource Presence Matrix
- Web Apps
- preflight + multi-instance update + verify

### Phase 4 — contest coverage

- Tasks
- Users/Roles/Resources
- Wallet/X509/OAuth presence
- System resources
- audit/event aggregation

### Phase 5 — Community Opportunity

- JDBC connections
- read-only Fleet Query

### Phase 6 — polish

- operation confirmations
- connection status
- error UX
- seeded demo scenario
- README/video/demo

---

## 32. References

### Contest

InterSystems Programming Contest: Build Your Own Management Portal  
https://community.intersystems.com/post/intersystems-programming-contest-build-your-own-management-portal

The contest asks for a GUI powered by IRIS management APIs covering Web Apps/REST, permission management, security/secrets, tasks, OS management and logs, and requires the app to work with IRIS Community Edition.

### Official SysAdmin API specification

Repository:  
https://github.com/intersystems-community/sysadmin-api-specification

OpenAPI specification:  
https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json

### Community Opportunity

InterSystems Ideas News #15:  
https://community.intersystems.com/post/intersystems-ideas-news-15

Open Exchange — “Querying from multiple IRIS instances” surfaced as waiting to be implemented:  
https://openexchange.intersystems.com/?tag=admin

Direct Ideas URL used for the opportunity:  
https://ideas.intersystems.com/ideas/DPI-I-598

### JDBC

InterSystems JDBC Driver Support:  
https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=BTPI_jdbc

InterSystems documents the driver as a pure Java Type 4 JDBC driver and documents the `jdbc:IRIS://host:port/namespace` connection format.

### Technology bonuses

https://community.intersystems.com/post/technology-bonuses-intersystems-programming-contest-build-your-own-management-portal

Relevant potential bonuses include Docker container usage, online demo and implementation of an InterSystems Community Idea.
