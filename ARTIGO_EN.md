# Building a Multi-Instance Management portal for InterSystems IRIS

## Introduction

InterSystems IRIS Management Portal administers one instance at a time. Teams responsible for several independent servers need a single view for process searches, configuration comparisons, monitoring, read-only SQL, and controlled changes.

IRIS Multi-Manager is a web portal for that workflow. It connects to a configured fleet of IRIS instances and preserves the source instance in every result. The application answers four operational questions:

- Where does a process or resource exist?
- On which instances is it missing?
- Which configurations differ?
- What was the result and duration on each selected server?

The implementation combines the IRIS SysAdmin API, the InterSystems JDBC driver, class dictionary metadata, ObjectScript SQL procedures, Embedded Python, `VECTOR`, `EMBEDDING`, and HNSW indexes. Angular provides the workspace. Quarkus handles authentication, bounded fan-out, validation, normalization, and per-target results.

## What the application does

The workspace contains eleven areas:

| Area | Implemented behavior |
| --- | --- |
| Instances | Connects independently to registered IRIS instances and stores credentials in a backend session |
| Overview | Shows health, uptime, process count, CSP sessions, performance, licensing, alerts, and short in-memory trends |
| Processes | Searches the fleet, qualifies each result by instance, and supports confirmed suspend, resume, and terminate actions |
| Licenses | Compares limits, current and high-water usage, users, and processes |
| System | Displays cumulative system counters and shared-memory allocation |
| Web Apps | Compares presence and configuration and applies supported changes after preflight |
| Tasks | Compares scheduled tasks and previews run, suspend, and resume operations |
| Permissions | Compares users, roles, resources, wallet collections, X509 credentials, and OAuth metadata |
| Events | Reads audit events per instance |
| Fleet Query | Executes one bounded, read-only `SELECT` on selected instances |
| Vector Search | Discovers vector assets, embedding configurations, storage locations, Embedded Python capabilities, source rows, and HNSW indexes |

Operations dispatched through `FleetExecutor` return an outcome for each selected target. Request validation and credential checks can reject a request before dispatch. Once execution starts, a target failure leaves successful results visible. Changes commit independently on each instance; a successful change remains applied when another target fails.

![Instances workspace showing three connected demo servers](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/screenshots/instances.png)

## Architecture

The system has a browser layer, an orchestration layer, and independent IRIS targets.

![Architecture and IRIS integration paths](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/01-architecture.png)

The frontend never calls managed IRIS servers directly. Nginx serves Angular and proxies `/api/` to Quarkus. The backend enforces instance registration, credentials, outbound destinations, timeouts, response limits, and mutation rules.

The two IRIS integration paths have distinct responsibilities:

1. **SysAdmin API** supplies administrative resources, monitoring, security metadata, tasks, processes, namespaces, and mappings.
2. **JDBC** executes bounded SQL, inspects the class dictionary and `%Embedding.Config`, previews source rows, runs HNSW DDL, and calls controlled SQL procedures.

The API proxy in [frontend/nginx.conf](frontend/nginx.conf) preserves the request path:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 65s;
    client_max_body_size 64k;
}
```

The browser opens port `8080` on the host. Nginx reaches `backend:8080` inside the Compose network; host port `8081` provides direct backend access. IRIS targets use service names such as `iris-prod1`, web port `52773`, and JDBC port `1972` on that network. The instance registry uses these internal addresses, independently of published host ports.

## Repository structure

```text
IRIS-multi-manager/
├── frontend/                  Angular standalone components and Nginx config
├── backend/                   Quarkus REST API, orchestration, validation, tests
├── docker/iris/               IRIS image, ObjectScript classes, routines, seed data
├── scripts/                   End-to-end PowerShell smoke tests
├── spec/                      Product and vector-management specifications
├── docs/screenshots/          UI captures used by the README and this article
└── docker-compose.yml         Five-service local demonstration topology
```

IRIS-specific responsibilities remain separated in the codebase. HTTP clients live under `sysadmin`, JDBC and dictionary code under `query` and `vector`, fleet behavior under `fleet`, and normalized resource models in their corresponding packages.

## Libraries and runtime technologies

| Layer | Technology | Role in the project |
| --- | --- | --- |
| IRIS | InterSystems IRIS Community 2026.2 | Managed data platform and three-instance demo runtime |
| IRIS code | ObjectScript classes and routines | Demo persistence, tasks, worker process, SQL procedures, vector extension |
| Python in IRIS | Embedded Python | Deterministic vector generation and Python module capability checks |
| IRIS SQL | `VECTOR`, `EMBEDDING`, `%Embedding.Config`, HNSW | Vector asset and embedding demonstration |
| Java backend | Java 21 | Records, HTTP/JDBC integration, concurrency, and validation |
| REST framework | Quarkus 3.27.5.1 / Jakarta REST | REST resources, dependency injection, validation, and health endpoints |
| JSON | Jackson | SysAdmin payload parsing, normalized responses, and safe field projection |
| Database access | InterSystems JDBC 3.11.0 | Fleet Query, dictionary discovery, vector actions, and SqlProc calls |
| Frontend | Angular 21.2, TypeScript 5.9 | Standalone components, signals, typed view models, and forms |
| Web server | Nginx 1.28 | Static Angular delivery and same-origin API proxy |
| Tests | JUnit 5, PowerShell smoke scripts | Unit safety checks and live multi-container validation |
| Packaging | Docker Compose | Reproducible frontend, backend, and three-IRIS demo |

Versions come from [backend/pom.xml](backend/pom.xml), [frontend/package.json](frontend/package.json), and the Dockerfiles. RxJS 7.8 and REST Assured are declared dependencies; the current application uses `fetch` and signals, and the tests use JUnit and PowerShell. Quarkus supplies Jakarta REST endpoints, dependency injection, Jackson JSON serialization, validation, OpenAPI support, and SmallRye health endpoints.

## The central model: every result belongs to an instance

A fleet response contains one `FleetTargetResult<T>` for each target:

```java
public record FleetTargetResult<T>(
    String instanceId,
    String instanceName,
    String status,
    int httpStatus,
    long elapsedMs,
    T data,
    String errorCode,
    String message) {}
```

The aggregate records requested and completed targets plus success and failure totals:

```java
public record FleetResult<T>(
    int requestedTargets,
    int completedTargets,
    long successCount,
    long failureCount,
    List<FleetTargetResult<T>> results) {}
```

This preserves the identity required for multi-instance operations. A PID, task name, or web application name can exist on several servers, so a process identity includes at least `(instanceId, pid)`. The backend carries that composite identity into Angular table keys and action URLs.

## Bounded parallel execution and partial failure

`FleetExecutor` is the shared orchestration component. It owns a fixed-size `ThreadPoolExecutor`, a bounded queue, a fleet deadline, and normalized error statuses.

![Parallel fleet execution with per-instance outcomes](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/02-fleet-execution.png)

The default configuration uses six workers, a queue of 64 tasks, and a 15-second fleet timeout. Network failures, authorization errors, unsupported operations, timeouts, and saturation become target statuses such as `OFFLINE`, `UNAUTHORIZED`, `FORBIDDEN`, `UNSUPPORTED`, `TIMEOUT`, and `BUSY`.

The executor submits each target to the shared pool and waits within the remaining fleet deadline. Queue saturation returns `BUSY`. At timeout it calls `Future.cancel(true)` and reports `TIMEOUT`; interruption requests cancellation, while completion of remote work depends on the client and server.

The pool in [FleetExecutor.java](backend/src/main/java/org/iris/multimanager/fleet/FleetExecutor.java) makes concurrency and queue capacity explicit:

```java
pool = new ThreadPoolExecutor(
    concurrency, concurrency, 0, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(64),
    Thread.ofPlatform().daemon().factory(),
    new ThreadPoolExecutor.AbortPolicy());
```

## Instance registry and session-scoped credentials

`application.properties` stores the instance registry as a JSON array. Each entry defines:

- internal ID and display name;
- SysAdmin base URL ending in `/api/admin`;
- environment label;
- JDBC host and port;
- default namespace;
- enabled flag.

`InstanceRegistry` validates the complete registry at startup. It accepts up to 32 instances, checks ID syntax, permits only HTTP and HTTPS, requires the exact `/api/admin` path, rejects URL user information, query strings, and fragments, and validates HTTP and JDBC hosts against `fleet.allowed-hosts`. This allowlist restricts outbound destinations and prevents browser-controlled input from turning the backend into an open network proxy.

The connection flow is:

![Instance authentication and session credentials](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/03-session-connection.png)

Credentials remain in the in-memory `SessionStore`, scoped by session ID and instance ID. Each valid session lookup renews its server-side expiration by 30 minutes. The browser cookie has a separate `Max-Age` of 1,800 seconds, set at connection time. The store accepts up to 256 sessions. `CredentialContext.toString()` omits the password from its string representation.

The SysAdmin client uses Basic Authentication, a 3-second connection timeout, a 10-second request timeout, disabled redirects, a 4 MB response limit, and a constrained set of administrative path patterns.

## Resource modeling and comparison matrices

IRIS administrative resources use different SysAdmin paths and identifiers. `ResourceCatalog` stores those differences as data:

```java
public record Definition(
    String listPath,
    String detailPath,
    String detailParameter,
    String key,
    Set<String> fields,
    Set<String> detailFields) {}
```

The catalog covers web applications, tasks, users, roles, resources, wallet collections, X509 credentials, OAuth servers, and OAuth resource servers. `ResourceService` retrieves each list and projects only fields allowed by `SafeMetadata`.

The matrix builder takes the union of keys returned by successful targets and creates one cell per instance. For each resource, the first available configuration becomes the comparison reference. Later configurations that differ receive `DIFFERENT`; matching configurations receive `PRESENT`. A successful response without the resource receives `MISSING`. Failed targets retain their operational status, such as `OFFLINE`.

![Resource comparison matrix construction](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/04-resource-comparison.png)

![Web Apps comparison matrix across the demo fleet](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/screenshots/web-apps.png)

## Safe mutation through preflight and confirmation

Web application and task changes use a two-step workflow. For a web application change, the backend:

1. validates the resource name, target count, property, type, and range;
2. reads the current configuration from every selected instance;
3. builds a before/after preview;
4. stores an immutable session-bound plan for two minutes;
5. consumes the plan once after confirmation;
6. reads the resource again to detect stale configuration;
7. writes only approved fields;
8. reads the resource again and verifies the result.

![Preview, confirmation, and verification of changes](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/05-confirmed-mutation.png)

Each `ConfirmedPlanStore` accepts up to 128 pending plans, binds them to their originating session, removes expired entries when adding plans, and consumes each token once. Web applications, tasks, and vector operations have separate stores. Web application changes support `Description` up to 256 characters, Boolean `Enabled`, and integer `Timeout` from 60 to 86,400.

Verification follows the operation. Web applications compare the preview with current configuration and read back changed fields. Tasks compare identity and configuration before execution; suspend and resume verify `Suspended`, while run reports asynchronous acceptance. HNSW execution returns after the DDL call, and Angular reloads inventory. Vector regeneration returns the affected row count.

Process control adds a process-identity check. The frontend reads `StartTimeUTC`, and the backend reads the process again immediately before suspend, resume, or terminate. A changed start time produces `STALE_PROCESS`, protecting against PID reuse.

![Processes workspace with instance-qualified search results](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/screenshots/processes.png)

## Monitoring model

The monitoring layer reads documented SysAdmin endpoints and converts payloads to typed records. `MonitorService` separates rates, cumulative counters, and current values:

- `GlobalRefsPerSecond` is a rate;
- `GlobalRefs`, `DiskReads`, and `DiskWrites` are counters since startup;
- process and CSP session counts are current values;
- license percentages remain `null` when IRIS returns no numeric value.

Health becomes `WARNING` when System Monitor is stopped or returned fields such as database space, journal, lock table, or write daemon are not `Normal`.

Angular refreshes Overview every 15 seconds while it is open. The backend stores up to 60 `MetricSample` values per instance in memory. The short trend chart therefore represents the current backend process and is cleared when that process restarts.

[MonitorService.java](backend/src/main/java/org/iris/multimanager/monitor/MonitorService.java) maps the endpoints to views:

| View | SysAdmin endpoint | Fields used |
| --- | --- | --- |
| Overview | `/v2/monitor/dashboard/main` | `Performance`, `SystemUsage`, `Status`, `Alerts`, `Licensing` |
| Licenses | `/v2/monitor/license-usage` | `UsageByUser`, `UsageByProcess` |
| System | `/v2/monitor/system-usage` | Global, routine, block, WIJ, and journal counters |
| Shared memory | `/v2/monitor/system-usage/shared-memory` | `SMHAllocated`, `SMHUsed`, `SMHAvailable`, `AllUsed` |

![Overview workspace with health cards, metrics, and trends](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/screenshots/overview.png)

## Fleet Query: read-only SQL across instances

Fleet Query uses the InterSystems JDBC driver and the credentials stored for each target. Before JDBC receives a statement, `SelectOnlyGuard` removes comments, accepts one optional trailing semicolon, requires `SELECT` as the first keyword, rejects additional semicolons, and blocks write or procedural keywords.

```java
if (normalized.isBlank()
    || normalized.contains(";")
    || !normalized.matches("(?is)^select\\b.*")) {
    throw new BadRequestException("Only one SELECT statement is allowed");
}
```

The service applies these bounds:

- maximum query length: 10,000 characters;
- configured maximum rows: 500;
- default timeout: 15 seconds;
- maximum accepted timeout: 60 seconds;
- JDBC `setMaxRows`, `setQueryTimeout`, and bounded fetch size;
- at most 32 selected targets.

The SQL guard performs lexical validation with regular expressions. IRIS account permissions remain the authorization boundary for database access. The query timeout and fleet deadline are separate: accepting a 60-second query timeout still leaves the default 15-second fleet deadline in effect.

### From the Angular request to JDBC

[FleetQuery](frontend/src/app/features/fleet-query.ts) sends the selected targets and bounds through the shared API function. This excerpt preserves the request and signal update:

```typescript
this.result.set(await api<QueryResult>('/query', {
  targets: this.selected(),
  sql: this.sql,
  maxRows: Number(this.maxRows),
  timeoutSeconds: 15
}));
```

The helper serializes the body as JSON and sends `POST /api/query`. Nginx forwards it to this endpoint in [QueryResource.java](backend/src/main/java/org/iris/multimanager/query/QueryResource.java), shown with formatting expanded:

```java
@Path("/api/query")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueryResource {
    @Inject FleetQueryService service;

    @POST
    public FleetQueryService.QueryResult query(
            @CookieParam("IRIS_SESSION") String session,
            FleetQueryService.QueryRequest request) {
        return service.execute(session, request);
    }
}
```

Quarkus maps JSON to `QueryRequest` through Jackson and injects `FleetQueryService`. The service validates SQL and limits, resolves targets, and checks every selected target's session credentials before dispatch. A missing credential at this stage rejects the request. The executor then calls this JDBC path for each target:

```java
String url = "jdbc:IRIS://" + instance.jdbcHost() + ":"
    + instance.jdbcPort() + "/" + instance.defaultNamespace();
Properties props = new Properties();
props.setProperty("user", credential.username());
props.setProperty("password", credential.password());
try (Connection connection = DriverManager.getConnection(url, props);
     Statement statement = connection.createStatement()) {
    statement.setQueryTimeout(timeout);
    statement.setMaxRows(maxRows);
    statement.setFetchSize(Math.min(maxRows, 100));
    try (ResultSet result = statement.executeQuery(sql)) {
        var metadata = result.getMetaData();
        var columns = new ArrayList<String>();
        for (int n = 1; n <= metadata.getColumnCount(); n++)
            columns.add(metadata.getColumnLabel(n));
        var rows = new ArrayList<List<JsonNode>>();
        while (result.next() && rows.size() < maxRows) {
            var row = new ArrayList<JsonNode>();
            for (int n = 1; n <= metadata.getColumnCount(); n++)
                row.add(value(result.getObject(n)));
            rows.add(List.copyOf(row));
        }
        return new QueryData(List.copyOf(columns), List.copyOf(rows));
    }
}
```

[FleetQueryService.java](backend/src/main/java/org/iris/multimanager/query/FleetQueryService.java) converts JDBC values to Jackson nodes: null, integer, decimal, Boolean, or text. Its `QueryResult` preserves fleet status fields and exposes `columns` and `rows` directly under each target. Angular updates the result signal and renders one table per instance.

![Fleet Query request from Angular to IRIS SQL](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/06-fleet-query.png)

![Fleet Query showing separate results for three servers](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/screenshots/fleet-query.png)

## Vector management inside the IRIS ecosystem

The vector workspace combines native IRIS metadata, SQL, ObjectScript, Embedded Python, and HNSW operations.

### Discovering `VECTOR` and `EMBEDDING`

The backend discovers SQL names and vector types through the class dictionary:

```sql
SELECT p.parent, p.Name, p.SqlFieldName, p.Type, p.Parameters,
       c.SqlTableName, c.SqlSchemaName
FROM %Dictionary.CompiledProperty p
JOIN %Dictionary.CompiledClass c ON c.ID = p.parent
WHERE p.Type IN ('%Library.Vector', '%Library.Embedding')
  AND LEFT(c.SqlSchemaName, 1) <> '%'
```

It uses `%Dictionary.CompiledIndex` for HNSW definitions, `%Dictionary.CompiledStorage` for data and index globals, and `%Embedding.Config` for embedding providers. Default namespace databases and global/package mappings from the SysAdmin API are combined with storage metadata to resolve the databases holding data, indexes, and code. The longest matching mapping wins. Unresolved locations are reported as `UNKNOWN`.

Embedding configuration is parsed as JSON and recursively sanitized. Keys such as `apiKey`, `token`, `password`, `secret`, and `clientSecret` become `[REDACTED]` before the response reaches Angular.

### ObjectScript SQL procedures and Embedded Python

`MultiManager.Vector.Extension` exposes controlled methods as SQL procedures. JDBC calls these procedures, and IRIS executes their implementations through Embedded Python.

```objectscript
ClassMethod CheckLibrary(moduleName As %String) As %String [ Language = python, SqlName = MM_VectorCheckLibrary, SqlProc ]
{
    import importlib.util
    import json
    import re

    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]{0,127}", moduleName or ""):
        raise ValueError("Use a top-level Python module name, such as sentence_transformers")
    try:
        available = importlib.util.find_spec(moduleName) is not None
        return json.dumps({"module": moduleName, "available": available,
                           "message": "Module found" if available else "Module not found"})
    except Exception:
        return json.dumps({"module": moduleName, "available": None,
                           "message": "Module discovery failed"})
}
```

This method comes from [Extension.cls](docker/iris/src/MultiManager/Vector/Extension.cls). `Language = python` selects the implementation language; `SqlProc` and `SqlName` expose it to SQL. Java binds the module name as a parameter in `SELECT MultiManager_Vector.MM_VectorCheckLibrary(?)`, then parses the returned JSON with Jackson.

`find_spec` checks module discovery. A successful discovery reports that the module can be located; imports, transitive dependencies, and model loading require separate checks. The extension also reports the Embedded Python version, tests a deterministic vector recipe, and supports regeneration. In this demo, `CheckModel` returns `ready: false`, and `DownloadModel` returns `downloaded: false`.

### Native `EMBEDDING` provider

The demo provider extends `%Embedding.Interface`. This excerpt shows the embedding method from [DemoEmbedding.cls](docker/iris/src/MultiManager/Vector/DemoEmbedding.cls); the class also implements configuration validation and a JSON helper:

```objectscript
Class MultiManager.Vector.DemoEmbedding Extends %Embedding.Interface
{
ClassMethod Embedding(input, configuration) As %Vector [ Language = python ]
{
    import hashlib, json
    digest = hashlib.sha256(str(input).encode("utf-8")).digest()
    values = [((digest[i] / 255.0) * 2.0) - 1.0 for i in range(4)]
    return json.dumps(values)
}
}
```

PROD-01 and PROD-02 contain an `EMBEDDING` column backed by this provider and an HNSW index. PROD-03 contains a direct `VECTOR(DOUBLE,4)` column without the initial HNSW index. The controlled difference makes fleet comparison possible without an external model or network dependency.

The provider maps four SHA-256 bytes to numeric values. These deterministic fixtures exercise storage and execution paths; their coordinates carry no learned semantic meaning. `sentence-transformers` is installed in the PROD-01 and PROD-02 images for capability checks. The vector recipe itself uses Python's standard library.

After registering `multimanager-demo` in `%Embedding.Config` with the provider class and vector length 4, [Seed.cls](docker/iris/src/MultiManager/Demo/Seed.cls) creates these SQL assets:

```sql
CREATE TABLE MultiManager_Vector.EmbeddingDocument (
    ID INTEGER IDENTITY PRIMARY KEY,
    Title VARCHAR(100),
    Content VARCHAR(1000),
    EmbeddingData EMBEDDING('multimanager-demo','Content')
);

CREATE INDEX EmbeddingHNSW
ON TABLE MultiManager_Vector.EmbeddingDocument (EmbeddingData)
AS HNSW(Distance='Cosine');
```

The seed explicitly computes fixture vectors with `EmbeddingJSON` and inserts them with `TO_VECTOR(?,DOUBLE)`. `EMBEDDING` retains the provider and source-column metadata that the inventory later reads.

### Controlled HNSW lifecycle

Create, rebuild, and drop operations resolve a discovered asset to its schema, table, and column. The requested index name passes syntax validation, and SQL identifiers are quoted. The browser receives the planned SQL and a confirmation phrase to type. Apply consumes a two-minute session-bound token and executes the stored plan. HNSW creation validates `M` from 2 to 100, `efConstruction` greater than `M` and at most 1,000, and uses `Cosine` or `DotProduct` distance.

Regeneration supports the four-dimensional direct `VECTOR` recipe. The UI selects row 1; the backend accepts a positive row ID, reads its `Content`, calls `MM_VectorRegenerate(?)`, and writes the returned JSON through `TO_VECTOR(?,DOUBLE)` with a parameterized row ID. This flow is implemented in [VectorService.java](backend/src/main/java/org/iris/multimanager/vector/VectorService.java).

![Vector regeneration through Embedded Python](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/07-vector-regeneration.png)

![Vector Search inventory with embedding and HNSW metadata](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/screenshots/vector-search.png)

## Angular implementation

The Angular frontend uses standalone components and typed interfaces that match backend records. A small `api<T>()` function sends same-origin requests with credentials, adds `X-Requested-With: IRIS-Multi-Manager`, and turns non-2xx responses into user-facing errors.

The transport in [api.ts](frontend/src/app/api.ts) uses the browser's `fetch` API. The request options are:

```typescript
const response = await fetch('/api' + path, {
  method: body === undefined ? 'GET' : 'POST',
  credentials: 'same-origin',
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'IRIS-Multi-Manager'
  },
  body: body === undefined ? undefined : JSON.stringify(body)
});
```

TypeScript describes the expected response shape at compile time. The helper returns `response.json()`; data validation and field projection run in the backend. Angular forms bind SQL, filters, and confirmation fields, while `signal()` stores results, progress, errors, and page selection.

Angular signals keep state local to each workspace. Shared components implement:

- instance selection;
- per-target result cards;
- resource matrices;
- before/after configuration comparison;
- metric bars and trend charts;
- reusable client-side pagination.

The UI preserves target boundaries. Fleet Query renders a separate section and paginator for each server, and resource matrices keep failure states in their original cells.

## Dockerized demonstration and data modeling

Docker Compose starts five services:

![Docker Compose services and persistent volumes](https://raw.githubusercontent.com/Davi-Massaru/IRIS-multi-manager/refs/heads/master/docs/diagrams/en/08-docker-topology.png)

Each IRIS instance has a durable volume. The custom IRIS image loads ObjectScript classes and a worker routine during the build. `start-demo.sh` recompiles the code and runs `MultiManager.Demo.Seed` at startup.

`DEMO_PROFILE` creates controlled differences:

- different counts of persistent `MultiManager.Demo.Person` rows;
- `/api/internal` missing from PROD-03;
- `DemoAudit` scheduled only on PROD-02;
- one demo worker process on every instance;
- different vector and embedding assets and row counts.

The persisted model in [Person.cls](docker/iris/src/MultiManager/Demo/Person.cls) is:

```objectscript
Class MultiManager.Demo.Person Extends %Persistent
{
Property Name As %String;
Property InstanceName As %String;
}
```

Embedded Python in `Seed.Run()` creates objects through the IRIS API. This excerpt runs inside the seed's first-run check:

```python
counts = {"PROD1": 3, "PROD2": 5, "PROD3": 7}
person_class = iris.cls("MultiManager.Demo.Person")
for number in range(1, counts[profile] + 1):
    person = person_class._New()
    person.Name = f"Person {number}"
    person.InstanceName = profile
    iris.check_status(person._Save())
```

IRIS projects those objects as `MultiManager_Demo.Person`, the table queried through JDBC. The `^MultiManagerDemo("seeded")` marker prevents repeated person and task creation on normal restarts. Persistent volumes retain data and previous demo changes.

The frontend image runs `npm ci` and `npm run build` in Node.js, then copies the bundle to Nginx. The backend image runs Maven `verify` with Java 21 and copies `quarkus-app` to a JRE image. Compose connects those images to three IRIS services, each with its own `DEMO_PROFILE` and `/durable` volume. Frontend and backend Dockerfiles define HTTP health checks; Compose checks each IRIS process with `iris qlist`.

## Security boundaries implemented in the code

The application enforces these controls:

- outbound HTTP and JDBC hosts must be registered and allowlisted;
- SysAdmin paths are constrained and redirects are disabled;
- credentials remain in an expiring backend session;
- requests other than GET, HEAD, and OPTIONS require `X-Requested-With: IRIS-Multi-Manager`;
- API responses use `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`;
- Nginx adds frame, referrer, and content-type headers;
- request bodies are capped at 64 KB;
- administrative metadata uses explicit field allowlists;
- secret-like embedding keys are recursively redacted;
- Fleet Query validates SELECT syntax and applies row and query-time limits;
- vector identifiers come from discovered metadata and strict validation;
- action plans expire, belong to a session, and can be consumed once;
- process mutations verify start time to detect PID reuse.

`ApiProtection` checks the custom header, and the browser uses a `SameSite=Strict` session cookie. IRIS permissions authorize the underlying operations. The [README](README.md#security-and-operational-notes) describes deployment requirements for frontend authentication, TLS, roles, secret management, network policy, and audit retention.

## Testing strategy

The backend image runs `mvn verify` during the multi-stage Docker build. Unit tests cover:

- partial-failure isolation and bounded concurrency;
- fleet timeouts and authentication error mapping;
- composite process identity;
- outbound URL validation;
- session and target credential scoping;
- resource safety and field allowlisting;
- mutation property validation;
- rejection of non-read-only SQL;
- vector identifier validation and recursive redaction;
- monitoring semantics and bounded metric history.

The PowerShell smoke suite exercises the live five-container environment. It authenticates against all three IRIS servers, searches processes, compares resources, tests monitoring and licensing, executes Fleet Query, verifies secret redaction, calls Embedded Python SQL procedures, regenerates a vector row, and creates then drops an HNSW index. It also verifies that a consumed vector plan cannot be replayed.

For example, [FleetExecutorTest.java](backend/src/test/java/org/iris/multimanager/fleet/FleetExecutorTest.java) simulates one unreachable target and checks successful results and source identity. The core assertions are:

```java
var result = executor.execute(targets(), i -> {
    if (i.id().equals("b")) throw new java.net.ConnectException();
    return 42;
});
assertEquals(2, result.successCount());
assertEquals("OFFLINE", result.results().get(1).status());
assertEquals("c", result.results().get(2).instanceId());
```

After starting the demo, run representative live checks from PowerShell:

```powershell
./scripts/smoke.ps1
./scripts/smoke-query.ps1
./scripts/smoke-webapps.ps1
./scripts/smoke-vector-library.ps1
./scripts/smoke-vector.ps1
```

These scripts operate on demo data. The web application check restores the original descriptions in `finally`; the vector check regenerates row 1 and creates then drops `SmokeHNSW`. The [README](README.md#validation-logs-and-shutdown) lists the complete suite.

## Example: from fleet request to operational result

### Check data and configuration across three servers

An administrator can use the demo to verify resource presence, compare data volume, and apply a scoped configuration change. Start the environment using the commands under **Running the project**, open [http://localhost:8080](http://localhost:8080), select PROD-01, PROD-02, and PROD-03 in **Instances**, and connect with `_SYSTEM` / `SYS`.

In **Fleet Query**, run:

```sql
SELECT InstanceName, COUNT(*) AS Total
FROM MultiManager_Demo.Person
GROUP BY InstanceName
```

A freshly seeded environment has these fixtures:

| Check | PROD-01 | PROD-02 | PROD-03 |
| --- | --- | --- | --- |
| `InstanceName` / person count | `PROD1` / 3 | `PROD2` / 5 | `PROD3` / 7 |
| `/api/internal` in Web Apps | Present | Present | Missing |
| `DemoAudit` in Tasks | Missing | Present | Missing |
| Vector asset | `EmbeddingDocument.EmbeddingData` | `EmbeddingDocument.EmbeddingData` | `Document.VectorData` |
| Vector rows | 3 | 4 | 5 |
| Initial HNSW index | `EmbeddingHNSW` | `EmbeddingHNSW` | None |

The UI shows SQL rows separately for each instance. Counts help verify the expected seed or investigate differences in data volume. Open **Web Apps** and **Tasks** to distinguish resource absence from configuration differences. Existing volumes can contain changes from earlier runs; compare their state with the initial fixtures above.

### Locate the process that owns the work

Open **Processes**, search for `DemoWorker`, and select **Details** on a returned row. A newly started demo has one worker per instance. The request follows this path:

1. Angular sends `GET /api/processes?instances=prod1,prod2,prod3&filter=DemoWorker`.
2. Quarkus resolves the three IDs through `InstanceRegistry`.
3. `FleetExecutor` runs one bounded SysAdmin request per target.
4. Each `/v2/processes` response receives `instanceId` and `instanceName`.
5. The backend returns one `FleetResult` with three independent outcomes.
6. Angular sorts and paginates successful rows while result cards retain each server's status and elapsed time.

The operator can associate process details with the server whose data or configuration needs investigation.

### Apply and verify a configuration change

Use the demo web application to exercise the complete preflight workflow:

1. In **Web Apps**, select only PROD-01 and PROD-02.
2. Open `/api/demo` and record each instance's current `Description`.
3. Choose `Description` and enter `Fleet validation example`.
4. Select **Preflight selected instances**. Inspect the before/after values and readiness for both targets. The preview performs reads only.
5. Confirm within two minutes and review each result. A `SUCCESS` result means the changed fields passed readback verification.
6. Reload the configuration to inspect the stored description. Restore each original value through a new preview and confirmation when finished.

The request built by the UI has this shape:

```json
{
  "targets": ["prod1", "prod2"],
  "name": "/api/demo",
  "changes": {"Description": "Fleet validation example"}
}
```

`POST /api/web-apps/preview` returns an `operationId`. Confirmation sends `{"confirmed":true}` to `POST /api/web-apps/execute/{operationId}`. A configuration change between those calls produces `STALE_CONFIGURATION` for the affected target. The operator can inspect that target and generate a new preview.

### Verify vector readiness

In **Vector Search**, select all three instances again and inspect the assets listed in the fixture table. Check `sentence_transformers` to compare module discovery across the three images, then use **Test** with sample text to execute the four-dimensional Python recipe.

For a controlled index change, locate PROD-03's `Document.VectorData`, select **Create HNSW**, review the SQL, and type the displayed confirmation phrase. The UI requests `VectorHNSW` with `Cosine`, `M=16`, and `efConstruction=64`, then reloads inventory after apply. Confirm that the index appears; **Drop** uses a new preview to remove the demonstration index when finished.

This exercise checks data, processes, configuration, Python availability, and index state from one workspace while retaining the outcome of each server.

## Impact within an IRIS environment

The code separates integration points that a developer can extend:

- Resource families use `ResourceCatalog` for endpoint paths, identity keys, and permitted fields, and `ResourceService` for retrieval and comparison.
- Additional servers enter through `fleet.instances` and `fleet.allowed-hosts` in [application.properties](backend/src/main/resources/application.properties), followed by a backend rebuild. The UI connects to the configured registry.
- JDBC operations use each instance's `defaultNamespace`. Vector inventory and capability checks require the supplied extension in that namespace; the demo installs it in `USER`.
- An embedding provider implements `%Embedding.Interface` and has a configuration in `%Embedding.Config`. Adapting the demo regeneration path to another model requires corresponding changes to its fixed recipe, dimensions, and source column.

These boundaries keep server registration, resource discovery, and provider implementation explicit in the project structure.

## Running the project

From the repository root:

```bash
docker compose up -d --build
docker compose ps
```

Use Docker with the Compose plugin and at least 8 GB of free memory available to Docker. The complete demo uses host ports `8080`, `8081`, `52773`–`52775`, and `1972`–`1974`. The first build downloads images and dependencies, including the Python package installation for PROD-01 and PROD-02.

After all five services report healthy, open [http://localhost:8080](http://localhost:8080) and connect to the three demo instances with `_SYSTEM` / `SYS`. These credentials belong to the local demo. The [README](README.md) contains installation, external-server registration, usage, and validation instructions.

## Conclusion

The demo connects object persistence, SQL queries, administrative APIs, and Embedded Python to a shared web interface. Its development patterns—instance identity, bounded execution, field projection, and preview plans—provide a basis for adding fleet operations with explicit scope and per-server results.
