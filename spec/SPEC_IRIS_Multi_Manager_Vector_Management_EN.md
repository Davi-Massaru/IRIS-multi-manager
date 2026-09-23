# SPEC-VM-002 — Vector Management Workspace for IRIS Multi-Manager

**STATUS:** PROPOSED — ENGLISH / TECHNICAL FEASIBILITY EDITION  
**PRIORITY:** P1 — CONTEST / COMMUNITY OPPORTUNITY  
**TARGET IDEA:** `DPI-I-557 — GUI for Vector DB Management`  
**PROJECT:** IRIS Multi-Manager  
**REPOSITORY:** https://github.com/Davi-Massaru/IRIS-multi-manager  
**ADMIN CONTRACT:** https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json  
**REFERENCE IRIS:** InterSystems IRIS Community 2026.2  
**BACKEND:** Quarkus / Java 21  
**FRONTEND:** Angular  
**DATABASE ACCESS:** InterSystems JDBC Driver  
**SERVER-SIDE EXTENSION:** InterSystems Embedded Python + `SqlProc`  
**RULE:** Vector Management is a workspace inside IRIS Multi-Manager. It is not a separate product.

---

# 0. COMMAND

## 0.1 MISSION

Add a **Vector Management** workspace to IRIS Multi-Manager.

The workspace SHALL allow an administrator to:

1. discover `VECTOR` and `EMBEDDING` resources across multiple IRIS instances;
2. identify:
   - instance;
   - namespace;
   - schema;
   - table;
   - column;
   - vector type;
   - dimensions;
   - embedding configuration;
   - generation recipe;
   - HNSW index;
3. resolve where the vector is logically visible and physically stored in the IRIS namespace/database model;
4. compare the same vector resource across instances;
5. inspect and manage embedding configurations;
6. verify local `SentenceTransformers` model readiness;
7. document how custom `VECTOR` columns are generated;
8. regenerate embeddings under controlled conditions;
9. list, create, rebuild and drop HNSW indexes;
10. display source data associated with vector records;
11. preserve the current Quarkus backend;
12. use Embedded Python only inside IRIS;
13. invoke Embedded Python through controlled stored procedures exposed with `SqlProc`;
14. use SysAdmin API v2 whenever the requested administrative capability exists in `mainspec_v2.json`.

---

## 0.2 COMMANDER'S INTENT

The final product SHALL remain:

> **one workspace for administering multiple InterSystems IRIS instances.**

Vector Management SHALL apply the same operating model already used by the Multi-Manager:

```text
FIND
  ↓
COMPARE
  ↓
SELECT TARGET INSTANCES
  ↓
PRE-FLIGHT
  ↓
APPLY
  ↓
VERIFY
  ↓
REPORT PER INSTANCE
```

The Vector workspace SHALL answer:

```text
WHERE DOES THIS VECTOR EXIST?

WHERE IS ITS DATA ACTUALLY STORED?

WHERE IS ITS CLASS / GENERATION CODE STORED?

HOW WAS THE VECTOR GENERATED?

IS THE MODEL READY ON EACH INSTANCE?

IS HNSW CONFIGURED THE SAME WAY?

CAN THIS EMBEDDING BE REGENERATED SAFELY?
```

---

## 0.3 NON-MISSION

DO NOT turn this module into:

- a chatbot;
- a RAG authoring platform;
- an LLM orchestration system;
- a Python remote shell;
- a generic SQL IDE;
- a separate vector observability product;
- a replacement for the main Multi-Manager.

---

# 1. SITUATION

## 1.1 CURRENT PROJECT BASELINE

The repository already contains:

- Angular frontend;
- Quarkus backend;
- Java 21;
- InterSystems JDBC Driver;
- three independent InterSystems IRIS Community instances in Docker;
- SysAdmin API integration;
- multi-instance Process Explorer;
- Resource Matrix;
- monitoring;
- license usage;
- Web Applications;
- Tasks;
- permissions/security;
- audit/events;
- Fleet Query through JDBC;
- partial-failure handling per instance.

Project references:

- Repository:
  https://github.com/Davi-Massaru/IRIS-multi-manager

- Backend Maven configuration:
  https://github.com/Davi-Massaru/IRIS-multi-manager/blob/master/backend/pom.xml

- Docker Compose:
  https://github.com/Davi-Massaru/IRIS-multi-manager/blob/master/docker-compose.yml

- IRIS image:
  https://github.com/Davi-Massaru/IRIS-multi-manager/blob/master/docker/iris/Dockerfile

- Demo loader:
  https://github.com/Davi-Massaru/IRIS-multi-manager/blob/master/docker/iris/start-demo.sh

### DECISION

**DO NOT migrate the backend to Python.**

Reason:

- the application is primarily a fleet orchestrator;
- Java already has official IRIS JDBC connectivity;
- Quarkus already exists in the repository;
- Python running outside IRIS does not qualify as Embedded Python;
- Embedded Python can be executed inside IRIS through controlled `SqlProc` methods.

---

## 1.2 COMMUNITY OPPORTUNITY

Target:

**DPI-I-557 — GUI for Vector DB Management**

Reference:

https://ideas.intersystems.com/ideas/DPI-I-557

Acceptance intent:

The implementation must provide a real GUI that allows administrators to understand and control vector data and the source data that produced those vectors.

Using Vector Search internally without a management UI is NOT sufficient.

---

# 2. CONTRACT BOUNDARY

## 2.1 PRIMARY RULE

Before implementing any administrative operation:

```text
IS THE CAPABILITY PRESENT IN mainspec_v2.json?
                │
          ┌─────┴─────┐
          │           │
         YES          NO
          │           │
          ▼           ▼
   SysAdmin API    Is it an IRIS SQL/
                   Vector/Search feature?
                        │
                   ┌────┴────┐
                   │         │
                  YES        NO
                   │         │
                   ▼         ▼
                 JDBC      DO NOT
                           INVENT
```

SysAdmin API specification:

https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json

The current specification is the authoritative contract for:

- instances;
- databases;
- namespaces;
- mappings;
- processes;
- Web Applications;
- Tasks;
- security;
- monitoring;
- licensing;
- other administrative resources already used by the project.

---

## 2.2 VECTOR SEARCH BOUNDARY

`mainspec_v2.json` does NOT expose dedicated Vector Search administration endpoints for:

- `VECTOR`;
- `EMBEDDING`;
- `%Embedding.Config`;
- HNSW;
- SentenceTransformers model lifecycle;
- embedding regeneration.

Therefore:

```text
SysAdmin API v2
    ↓
namespace / database / mapping context

JDBC / SQL
    ↓
VECTOR / EMBEDDING / metadata / HNSW

JDBC CALL
    ↓
SqlProc

SqlProc
    ↓
Embedded Python
```

The README and UI SHALL make this boundary explicit.

---

# 3. TECHNICAL FEASIBILITY — MASTER MATRIX

| Capability | Primary mechanism | IRIS source | Java implementation | Feasibility | Main constraint |
|---|---|---|---|---|---|
| Instance capability check | SysAdmin REST | `/api/admin/info` | Quarkus REST Client | HIGH | auth / availability |
| Database inventory | SysAdmin REST | `/v2/databases` | Quarkus REST Client | HIGH | permission |
| Namespace inventory | SysAdmin REST | `/v2/namespaces` | Quarkus REST Client | HIGH | `%Admin_Manage` |
| Global mappings | SysAdmin REST | `/v2/namespace/global-mappings` | Quarkus REST Client | HIGH | namespace permission |
| Routine mappings | SysAdmin REST | `/v2/namespace/routine-mappings` | Quarkus REST Client | HIGH | namespace permission |
| Package mappings | SysAdmin REST | `/v2/namespace/package-mappings` | Quarkus REST Client | HIGH | namespace permission |
| Discover VECTOR columns | JDBC metadata / SQL | `INFORMATION_SCHEMA.COLUMNS` | JDBC repository | HIGH | validate datatype representation |
| Discover EMBEDDING columns | JDBC metadata / SQL | SQL metadata | JDBC repository | HIGH | IRIS 2026.2 validation |
| Embedding configs | JDBC SQL | `%Embedding.Config` | JDBC repository | HIGH | secrets must be sanitized |
| SentenceTransformers readiness | JDBC CALL | custom `SqlProc` | `CallableStatement` | HIGH | extension must exist |
| Embedded Python runtime info | JDBC CALL | custom `SqlProc` | `CallableStatement` | HIGH | extension must exist |
| Download local model | JDBC CALL | `%Embedding.SentenceTransformers` through controlled extension | `CallableStatement` | MEDIUM/HIGH | network/cache/token |
| Test embedding generation | JDBC CALL | Embedded Python | `CallableStatement` | HIGH | model must be ready |
| Regenerate selected vectors | JDBC CALL + SQL | custom recipe + Embedded Python | service + stored procedure | MEDIUM | write operation / batching |
| HNSW inventory | JDBC SQL metadata | `INFORMATION_SCHEMA.INDEXES`, `%Dictionary.CompiledIndex` | JDBC repository | MEDIUM/HIGH | exact metadata query must be tested |
| HNSW create | JDBC SQL | `CREATE INDEX ... AS HNSW` | prepared/validated SQL builder | HIGH | supported column/storage |
| HNSW rebuild | JDBC SQL | `BUILD INDEX` | JDBC service | HIGH | privilege / lock/load |
| HNSW drop | JDBC SQL | `DROP INDEX` | JDBC service | HIGH | destructive action |
| Source-data explorer | JDBC SELECT | application table | paged JDBC query | HIGH | sensitive data / pagination |
| Vector preview | JDBC SELECT | vector column | limited on-demand query | HIGH | never fetch all vectors |
| Vector location resolution | SysAdmin REST + JDBC | Namespace + mappings + Storage Definition | resolver service | HIGH | custom storage may be complex |
| Shared physical dataset detection | resolver | `(instance, database, global)` | Java dedup logic | HIGH | never dedup cross-instance |
| Growth history | Multi-Manager snapshots | current metadata + counts | Quarkus scheduler/storage | MEDIUM | no historical data before first sample |

---

# 4. AUTHORIZED IRIS ADMIN SOURCES

## 4.1 INSTANCE INFO

### USE

```http
GET /api/admin/info
```

### PURPOSE

- check instance availability;
- identify API/server context;
- verify authenticated access;
- populate Vector workspace capability status.

### SOURCE

`mainspec_v2.json` → `/info`

### FEASIBILITY

**HIGH**

Already compatible with the current Multi-Manager architecture.

---

## 4.2 DATABASES

### USE

```http
GET /api/admin/v2/databases
```

When required:

```http
GET /api/admin/v2/database?name={database}
GET /api/admin/v2/database-dir?... 
```

### PURPOSE

- list IRIS databases;
- identify physical database names referenced by namespace/mapping resolution;
- link Vector assets back to the main Database administration area.

### SOURCE

`mainspec_v2.json`:

- `/v2/databases`
- `/v2/database`
- `/v2/database-dir`

### FEASIBILITY

**HIGH**

No custom IRIS code required.

---

## 4.3 NAMESPACES

### USE

```http
GET /api/admin/v2/namespaces
```

### PURPOSE

The `NamespaceList` schema exposes administrative context including:

```text
Name
Globals
Routines
SysGlobals
SysRoutines
Library
TempGlobals
```

This allows the UI to show:

```text
Namespace             APP
Default Globals DB    APPDATA
Default Routines DB   APPCODE
System Globals DB     IRISSYS
System Routines DB    IRISSYS
Library DB            IRISLIB
Temp Globals DB       IRISTEMP
```

### OFFICIAL IRIS DOCUMENTATION

CreateNamespace:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=RACS_createnamespace

The documentation defines:

- `Globals` as the default globals database;
- `Routines` as the default routines database;
- `TempGlobals` as the default temporary-globals database.

### FEASIBILITY

**HIGH**

This data is available directly from SysAdmin API v2.

---

# 5. VECTOR LOCATION RESOLUTION

## 5.1 OPERATIONAL REQUIREMENT

For every vector asset, distinguish:

```text
LOGICAL VISIBILITY
        ≠
PHYSICAL DATA STORAGE
        ≠
CODE STORAGE
```

The UI SHALL NOT display an ambiguous field called only:

```text
Database
```

Instead, use explicit terminology:

```text
Logical Namespace
Default Globals DB
Effective Data DB
Effective Index DB
Default Routines DB
Effective Code DB
```

---

## 5.2 IRIS STORAGE MODEL

A persistent class can use different globals for:

```text
DataLocation
IdLocation
IndexLocation
StreamLocation
```

Official documentation:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GOBJ_storage

The official documentation shows storage definitions containing:

```text
DataLocation
IdLocation
IndexLocation
StreamLocation
```

Therefore:

```text
table/class
    │
    ├── data global
    ├── index global
    └── stream global
```

must be resolved independently.

### FEASIBILITY

**HIGH**, with one condition:

The exact storage definition query/access method must be validated in the IRIS 2026.2 demo image.

Do NOT infer a global only from class naming convention.

---

## 5.3 GLOBAL MAPPINGS

### USE

```http
GET /api/admin/v2/namespace/global-mappings?namespace={namespace}
```

Optional detail:

```http
GET /api/admin/v2/namespace/global-mapping
```

### PURPOSE

Resolve whether a data/index global is stored in a database different from the namespace default `Globals` database.

Example:

```text
Namespace APP
Default Globals DB = APPDATA

DataLocation:
^RAG.ChunkD

Mapping:
^RAG.ChunkD → VECTORDB

Effective Data DB:
VECTORDB
```

### OFFICIAL DOCUMENTATION

Namespace mappings:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GSA_config_namespace_addmap

Global mapping:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GGBL_mapping

### FEASIBILITY

**HIGH**

The mapping inventory is available from SysAdmin API v2.

---

## 5.4 ROUTINE MAPPINGS

### USE

```http
GET /api/admin/v2/namespace/routine-mappings?namespace={namespace}
```

### PURPOSE

Resolve routine code that is not stored in the namespace default `Routines` database.

### FEASIBILITY

**HIGH**

Use only when routine-level code resolution is needed.

---

## 5.5 PACKAGE MAPPINGS

### USE

```http
GET /api/admin/v2/namespace/package-mappings?namespace={namespace}
```

### PURPOSE

Resolve the database that provides classes/packages visible in a namespace.

This is especially relevant for:

```text
MultiManager.Vector
RAG.*
Embedding helper classes
```

### CRITICAL RULE

A package mapping can change **where class code is found**.

It does NOT automatically change where persistent data is stored.

Therefore this is valid:

```text
Class code:
SHARED-CODE

Persistent vector data:
VECTORDB
```

### FEASIBILITY

**HIGH**

Mapping comes from SysAdmin API v2.

---

# 6. LOCATION RESOLUTION ALGORITHM

## 6.1 INPUT

```text
instance
namespace
schema
table
vectorColumn
```

---

## 6.2 DATA PATH

### STEP 1 — Namespace defaults

Use:

```http
GET /v2/namespaces
```

Read:

```text
defaultGlobalsDatabase
defaultRoutinesDatabase
```

### STEP 2 — Storage definition

Use JDBC / IRIS class metadata to obtain:

```text
persistentClass
dataGlobal
idGlobal
indexGlobal
streamGlobal
```

### STEP 3 — Mapping inventory

Use:

```http
GET /v2/namespace/global-mappings
```

### STEP 4 — Resolve data global

If a mapping applies:

```text
effectiveDataDatabase = mapping.Database
source = GLOBAL_MAPPING
```

Otherwise:

```text
effectiveDataDatabase = namespace.Globals
source = NAMESPACE_DEFAULT
```

### STEP 5 — Resolve index global separately

If mapping applies:

```text
effectiveIndexDatabase = mapping.Database
```

Otherwise:

```text
effectiveIndexDatabase = namespace.Globals
```

### STEP 6 — Resolve code

Use package/routine mappings.

If package mapping applies:

```text
effectiveCodeDatabase = packageMapping.Database
```

Otherwise:

```text
effectiveCodeDatabase = namespace.Routines
```

---

## 6.3 RESULT MODEL

```text
VectorLocation
--------------
instanceId
namespace

defaultGlobalsDatabase
defaultRoutinesDatabase
tempGlobalsDatabase
systemGlobalsDatabase
systemRoutinesDatabase
libraryDatabase

schema
table
column
persistentClass
packageName

dataGlobal
idGlobal
indexGlobal
streamGlobal

effectiveDataDatabase
effectiveIndexDatabase
effectiveStreamDatabase
effectiveCodeDatabase
effectiveRoutineDatabase

dataLocationSource
indexLocationSource
codeLocationSource

globalMappings[]
packageMappings[]
routineMappings[]

sharedPhysicalData
logicalExposureCount
```

Enum:

```text
LocationSource
--------------
NAMESPACE_DEFAULT
GLOBAL_MAPPING
PACKAGE_MAPPING
ROUTINE_MAPPING
SYSTEM_MAPPING
UNKNOWN
```

---

## 6.4 TECHNICAL FEASIBILITY

| Part | What to use | Feasibility |
|---|---|---|
| namespace defaults | SysAdmin `/v2/namespaces` | HIGH |
| database inventory | SysAdmin `/v2/databases` | HIGH |
| global mapping | SysAdmin `/v2/namespace/global-mappings` | HIGH |
| routine mapping | SysAdmin `/v2/namespace/routine-mappings` | HIGH |
| package mapping | SysAdmin `/v2/namespace/package-mappings` | HIGH |
| class/table identification | JDBC SQL metadata | HIGH |
| storage definition | IRIS class/storage metadata | MEDIUM/HIGH |
| data-global resolution | Java resolver | HIGH |
| code-database resolution | Java resolver | HIGH |
| physical dedup | Java tuple comparison | HIGH |

---

# 7. VECTOR INVENTORY

## 7.1 DISCOVERY

For every:

```text
(instance, namespace)
```

use JDBC to query metadata.

Primary source:

```text
INFORMATION_SCHEMA.COLUMNS
```

Official documentation:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=INFORMATION.SCHEMA.COLUMNS&LIBRARY=%25SYS

Discover columns of:

```text
VECTOR
EMBEDDING
```

### RETURN

```text
instance
namespace
schema
table
column
type
dimensions
```

### FEASIBILITY

**HIGH**

Validation task:

Confirm the exact datatype metadata representation returned by IRIS 2026.2.

---

## 7.2 ROW COUNTS

For each asset compute:

```text
total rows
rows where vector column IS NOT NULL
```

### RULES

- never `SELECT *`;
- never fetch vector payload for counts;
- enforce timeout;
- execute per selected namespace;
- aggregate per instance.

### FEASIBILITY

**HIGH**

---

# 8. VECTOR WORKSPACE UI

## 8.1 MAIN SCREEN

```text
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ VECTOR SEARCH                                                                        │
│ Inventory | Models | Generation | Indexes                                            │
├──────────────────────────────────────────────────────────────────────────────────────┤
│ Instances: [✓ PROD-01] [✓ PROD-02] [✓ PROD-03]      Namespace: [ ALL ▼ ]            │
├──────────────────────────────────────────────────────────────────────────────────────┤
│ CAPABILITIES                                                                         │
│                                                                                      │
│                     PROD-01             PROD-02             PROD-03                  │
│ JDBC                    ✓                   ✓                   ✓                     │
│ Vector SQL              ✓                   ✓                   ✓                     │
│ MM Extension            ✓                   ✓                   ✕                     │
│ Embedded Python         ✓                   ✓                   -                     │
│ sentence-transformers   ✓                   ✕                   -                     │
├──────────────────────────────────────────────────────────────────────────────────────┤
│ VECTOR ASSETS                                                                        │
│                                                                                      │
│ INSTANCE NS   TABLE       COLUMN      TYPE      DIM MODEL      DATA DB    CODE DB     │
│ PROD-01  APP  RAG.Chunk   Embedding   EMBEDDING 384 miniLM     VECTORDB   APPCODE     │
│ PROD-02  APP  RAG.Chunk   Embedding   EMBEDDING 384 miniLM     VECTORDB   APPCODE     │
│ PROD-03  APP  RAG.Chunk   Embedding   VECTOR    384 recipe-v2  APPDATA    APP         │
├──────────────────────────────────────────────────────────────────────────────────────┤
│ SELECTED: RAG.Chunk.Embedding                                                         │
│                                                                                      │
│                        PROD-01            PROD-02            PROD-03                  │
│ Type                   EMBEDDING          EMBEDDING          VECTOR                   │
│ Dimensions             384                384                384                      │
│ Model/config           miniLM             miniLM             recipe-v2       ⚠        │
│ HNSW                   ✓                  ✓                  ✕               ⚠        │
│ Distance               Cosine             Cosine             -                        │
│ M                      16                 16                 -                        │
│ efConstruction         64                 64                 -                        │
│ Rows w/vector          120,310            120,290            119,991                  │
│                                                                                      │
│ STORAGE LOCATION                                                                     │
│ Namespace              APP                APP                APP                      │
│ Default Globals DB     APPDATA            APPDATA            APPDATA                  │
│ Data global            ^RAG.ChunkD        ^RAG.ChunkD        ^RAG.ChunkD              │
│ Effective Data DB      VECTORDB [MAP]     VECTORDB [MAP]     APPDATA                  │
│ Index global           ^RAG.ChunkI        ^RAG.ChunkI        ^RAG.ChunkI              │
│ Effective Index DB     VECTORINDEX [MAP]  VECTORINDEX [MAP]  APPDATA                  │
│ Default Routines DB    APPCODE            APPCODE            APP                      │
│ Effective Code DB      SHARED-CODE [MAP]  SHARED-CODE [MAP]  APP                      │
│                                                                                      │
│ [Explore] [Location] [Compare] [Generation] [Index]                                  │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

# 9. LOCATION DETAIL SCREEN

```text
┌──────────────────────────────────────────────────────────────────────┐
│ LOCATION — RAG.Chunk.Embedding                                      │
├──────────────────────────────────────────────────────────────────────┤
│ INSTANCE                                                            │
│ PROD-01                                                             │
├──────────────────────────────────────────────────────────────────────┤
│ LOGICAL CONTEXT                                                     │
│ Namespace                    APP                                    │
│ Schema                       RAG                                    │
│ Table                        Chunk                                  │
│ Column                       Embedding                              │
│ Persistent class             RAG.Chunk                              │
├──────────────────────────────────────────────────────────────────────┤
│ NAMESPACE DEFAULTS                                                  │
│ Globals database             APPDATA                                │
│ Routines database            APPCODE                                │
│ Temp globals                 IRISTEMP                               │
│ System globals               IRISSYS                                │
│ System routines              IRISSYS                                │
│ Library                      IRISLIB                                │
├──────────────────────────────────────────────────────────────────────┤
│ DATA STORAGE                                                        │
│ Data global                  ^RAG.ChunkD                            │
│ Default location             APPDATA                                │
│ Global mapping               ^RAG.ChunkD → VECTORDB                │
│ Effective physical DB        VECTORDB                    [MAPPED]    │
│                                                                      │
│ Index global                 ^RAG.ChunkI                            │
│ Global mapping               ^RAG.ChunkI → VECTORINDEX             │
│ Effective index DB           VECTORINDEX                 [MAPPED]    │
├──────────────────────────────────────────────────────────────────────┤
│ CODE STORAGE                                                        │
│ Package                      RAG                                    │
│ Default routines DB          APPCODE                                │
│ Package mapping              RAG → SHARED-CODE                     │
│ Effective code DB            SHARED-CODE                 [MAPPED]    │
├──────────────────────────────────────────────────────────────────────┤
│ LOGICAL EXPOSURES                                                   │
│ APP                  → VECTORDB:^RAG.ChunkD                         │
│ REPORTING            → VECTORDB:^RAG.ChunkD                         │
│                                                                      │
│ Logical exposures: 2                                                │
│ Physical dataset:  1                                                │
└──────────────────────────────────────────────────────────────────────┘
```

---

# 10. SHARED PHYSICAL DATASET DETECTION

## 10.1 PHYSICAL IDENTITY

Within one IRIS instance:

```text
PhysicalDatasetId =
(instanceId, effectiveDatabase, globalRoot)
```

Example:

```text
PROD-01 / VECTORDB / ^RAG.ChunkD
```

If:

```text
APP       → VECTORDB:^RAG.ChunkD
REPORTING → VECTORDB:^RAG.ChunkD
```

then:

```text
logicalExposureCount = 2
physicalDatasetCount = 1
```

### RULE

Never deduplicate across different instances.

These are distinct physical systems:

```text
PROD-01 / VECTORDB / ^RAG.ChunkD
PROD-02 / VECTORDB / ^RAG.ChunkD
```

even when names match.

### FEASIBILITY

**HIGH**

Pure Java comparison after location resolution.

---

# 11. EMBEDDING CONFIGURATION MANAGEMENT

## 11.1 SOURCE

Use:

```text
%Embedding.Config
```

Official Vector Search documentation:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GSQL_vecsearch

The official documentation describes configuration metadata including:

```text
Name
Configuration
EmbeddingClass
VectorLength
Description
```

Built-in classes include:

```text
%Embedding.OpenAI
%Embedding.SentenceTransformers
```

### MECHANISM

JDBC SQL.

### FEASIBILITY

**HIGH**

---

## 11.2 SECRET SANITIZATION

`Configuration` can contain sensitive values such as:

```text
apiKey
```

The backend SHALL transform:

```json
{
  "modelName": "text-embedding-3-small",
  "sslConfig": "llm_ssl",
  "apiKey": "secret"
}
```

into:

```json
{
  "modelName": "text-embedding-3-small",
  "sslConfig": "llm_ssl",
  "hasApiKey": true
}
```

The browser SHALL never receive the secret.

### FEASIBILITY

**HIGH**

Java JSON sanitization.

---

# 12. MODEL CONTROL

## 12.1 PURPOSE

Answer:

```text
Which embedding config exists?
Which model is referenced?
Is sentence-transformers installed?
Is the model cache available?
What vector length is expected?
Does the state differ across instances?
```

---

## 12.2 MODEL MATRIX

```text
MODEL: miniLM

                       PROD-01        PROD-02        PROD-03

Config exists              ✓              ✓              ✓
Class             SentenceTransf. SentenceTransf. SentenceTransf.
Model             all-MiniLM      all-MiniLM      all-MiniLM
Vector length             384            384            384
Python package              ✓              ✕              ✓
Model cached                ✓              ✕              ✓
```

Actions:

```text
[Check selected instances]
[Download on PROD-02]
[Test selected instances]
```

---

# 13. EMBEDDED PYTHON EXTENSION

## 13.1 WHY

Embedded Python is required only for operations that must execute locally in the IRIS process environment, for example:

- check Python runtime;
- check `sentence_transformers`;
- inspect model/cache readiness;
- generate an embedding locally;
- regenerate custom vectors.

Python running in Quarkus/FastAPI outside IRIS SHALL NOT be used as a substitute.

---

## 13.2 OFFICIAL SUPPORT

InterSystems IRIS supports method implementation with:

```text
Language = python
```

Official reference:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ROBJ_method_language

InterSystems IRIS supports exposing class methods as SQL stored procedures using:

```text
SqlProc
```

Official reference:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ROBJ_method_sqlproc

### FEASIBILITY

**HIGH**

This is a native IRIS mechanism.

---

# 14. MULTI-MANAGER VECTOR EXTENSION

## 14.1 FORM

Proposed class:

```text
MultiManager.Vector.Extension
```

It SHALL NOT:

- run as a daemon;
- open a listener;
- poll independently;
- execute arbitrary Python from user input.

It SHALL expose controlled operations.

---

## 14.2 PROCEDURES

### `MM_VectorCapabilities`

**USE:** Embedded Python + `SqlProc`

Returns:

```json
{
  "extensionVersion": "0.1.0",
  "pythonVersion": "3.x",
  "sentenceTransformers": true,
  "namespace": "USER"
}
```

**FEASIBILITY:** HIGH

---

### `MM_VectorCheckModel`

Input:

```text
embeddingConfigName
```

Returns:

```json
{
  "config": "miniLM",
  "model": "sentence-transformers/all-MiniLM-L6-v2",
  "installed": true,
  "vectorLength": 384,
  "maxTokens": 256
}
```

**USE:**

- `%Embedding.Config`;
- `%Embedding.SentenceTransformers`;
- Embedded Python.

**FEASIBILITY:** HIGH

---

### `MM_VectorDownloadModel`

Input:

```text
embeddingConfigName
temporaryToken?
```

Responsibility:

- only supported for local SentenceTransformers configuration;
- download/cache the configured model;
- verify readiness after completion.

**FEASIBILITY:** MEDIUM/HIGH

Constraints:

- network access;
- disk capacity;
- Hugging Face authentication when needed;
- potentially long operation.

---

### `MM_VectorTestModel`

Input:

```text
configName
testText
```

Return:

```text
success
dimensions
elapsedMillis
sanitizedError
```

Do NOT return full vector unless explicitly requested by a debug/admin operation.

**FEASIBILITY:** HIGH

---

### `MM_VectorRegenerate`

Input:

```text
recipeName
scope
ids
```

Scopes:

```text
SELECTED_IDS
ALL
```

MVP SHALL implement:

```text
SELECTED_IDS
```

first.

**FEASIBILITY:** MEDIUM

Constraints:

- write operation;
- potentially expensive;
- must run in batches;
- must validate output dimension.

---

# 15. JAVA INVOCATION

## 15.1 JDBC CALL

Java invokes the stored procedures with JDBC `CallableStatement`.

Official reference:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25XDBC.Gateway.JDBC.CallableStatement&LIBRARY=%25SYS

Architecture:

```text
Angular
   ↓
Quarkus REST
   ↓
VectorExtensionService
   ↓
JDBC CallableStatement
   ↓
IRIS SqlProc
   ↓
Embedded Python
```

Conceptual Java:

```java
try (CallableStatement stmt =
        connection.prepareCall("{? = call MM_VectorCapabilities()}")) {
    stmt.registerOutParameter(1, Types.VARCHAR);
    stmt.execute();
    String json = stmt.getString(1);
}
```

### FEASIBILITY

**HIGH**

The project already includes the IRIS JDBC driver.

---

# 16. MODEL / PACKAGE INSTALLATION

## 16.1 PYTHON PACKAGES

Official Embedded Python package-install documentation:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GEPYTHON_loadlib

For the Docker demo, install packages in the IRIS Python target directory supported by the image.

Example concept:

```dockerfile
RUN python3 -m pip install \
    --target /usr/irissys/mgr/python \
    sentence-transformers==<validated-version>
```

### RULE

Version pinning is mandatory.

Never depend on floating `latest`.

### FEASIBILITY

**HIGH** for demo/container builds.

---

# 17. GENERATION RECIPES

## 17.1 PROBLEM

Many real systems use:

```text
source text
   ↓
Python script
   ↓
embedding model
   ↓
VECTOR column
```

The database may contain the result but not enough information to answer:

```text
Which script generated this?
Which model?
Which source column?
Which version?
How can it be regenerated?
```

---

## 17.2 NATIVE EMBEDDING CASE

When a column uses IRIS native `EMBEDDING`, the definition includes model/source semantics.

Official documentation:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GSQL_vecsearch

Classification:

```text
GENERATION = IRIS_NATIVE
```

A custom recipe is not mandatory.

---

## 17.3 CUSTOM VECTOR CASE

A plain `VECTOR` can be generated by arbitrary application code.

Default classification:

```text
GENERATION = UNKNOWN
```

The administrator may associate a controlled recipe.

---

## 17.4 RECIPE MODEL

```text
EmbeddingRecipe
---------------
name
version
schema
table
idColumn
sourceColumns[]
vectorColumn
strategy
embeddingConfig
batchSize
checksum
description
```

Allowed MVP strategies:

```text
IRIS_EMBEDDING
SENTENCE_TRANSFORMERS
UNKNOWN
```

Do NOT allow arbitrary Python source as a recipe.

---

# 18. GENERATION SCREEN

```text
┌────────────────────────────────────────────────────────────────────┐
│ GENERATION — RAG.Chunk.Embedding                                   │
├────────────────────────────────────────────────────────────────────┤
│ Instance        PROD-03                                            │
│ Type            VECTOR                                             │
│ Recipe          chunk-minilm-v2                                    │
│ Version         2                                                  │
│ Source          RAG.Chunk.Content                                  │
│ Target          RAG.Chunk.Embedding                                │
│ Config          miniLM                                             │
│ Model           sentence-transformers/all-MiniLM-L6-v2             │
│ Batch size      250                                                │
│ Status          READY                                              │
│                                                                    │
│ [Test one record] [Regenerate selected] [Regenerate all]           │
└────────────────────────────────────────────────────────────────────┘
```

---

# 19. REGENERATION PROCEDURE

## 19.1 CONTROL FLOW

Every regeneration SHALL use:

```text
PRE-FLIGHT
   ↓
PREVIEW
   ↓
CONFIRM
   ↓
EXECUTE
   ↓
VERIFY
   ↓
RESULT PER INSTANCE
```

---

## 19.2 BATCH RULES

The procedure SHALL:

- process bounded batches;
- never load the entire table in memory;
- validate vector length;
- count successes;
- count failures;
- return per-row error details when practical;
- stop or continue according to explicit policy.

### FEASIBILITY

**MEDIUM**

Technically feasible, but write load and transaction boundaries require testing.

---

# 20. HNSW MANAGEMENT

## 20.1 OFFICIAL IRIS SUPPORT

InterSystems IRIS 2026.2 supports HNSW vector indexes.

Official documentation:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GSQL_vecsearch

The documentation defines:

```text
Distance
M
efConstruction
```

and supports:

```text
Cosine
DotProduct
```

The documentation states that HNSW can be created using:

```sql
CREATE INDEX ...
AS HNSW(...)
```

---

## 20.2 HNSW INVENTORY

Primary sources:

```text
INFORMATION_SCHEMA.INDEXES
%Dictionary.CompiledIndex
```

Official documentation:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=INFORMATION.SCHEMA.INDEXES

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25Dictionary.CompiledIndex&LIBRARY=%25SYS

### FEASIBILITY

**MEDIUM/HIGH**

The exact query used to identify HNSW-specific metadata must be validated against the 2026.2 demo.

---

## 20.3 HNSW ACTIONS

Supported UI actions:

```text
List
Create
Rebuild
Drop
```

### CREATE

Preview:

```sql
CREATE INDEX ChunkHNSW
ON TABLE RAG.Chunk (Embedding)
AS HNSW(
  M=16,
  efConstruction=64,
  Distance='Cosine'
)
```

### REBUILD

Use IRIS:

```text
BUILD INDEX
```

Official documentation:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=RSQL_buildindex

### SAFETY

Identifiers SHALL come from discovered metadata.

Do not concatenate unrestricted browser input into DDL.

### FEASIBILITY

**HIGH**

Subject to IRIS HNSW restrictions documented by InterSystems.

---

# 21. SOURCE DATA EXPLORER

## 21.1 PURPOSE

Directly address the DPI-I-557 problem:

> What original data produced this vector?

---

## 21.2 UI

```text
VECTOR DATA — RAG.Chunk.Embedding

ID     SOURCE PREVIEW                         VECTOR
101    "OAuth token validation..."           384 dims
102    "IRIS database performance..."         384 dims
103    "FHIR search parameters..."            384 dims

[Open]
```

Detail:

```text
Record        101
Source        OAuth token validation failed...
Vector type   FLOAT
Dimensions    384
Generator     miniLM / recipe-v2

Vector preview
[0.013, -0.283, 0.019, ...]

[Regenerate this record]
```

### RULES

- paged query;
- limited source preview;
- vector preview only on demand;
- never fetch all vector elements for all rows;
- respect sensitive application data.

### FEASIBILITY

**HIGH**

---

# 22. STORAGE COMPARISON

## 22.1 MATRIX

```text
RAG.Chunk.Embedding

                          PROD-01        PROD-02       PROD-03

Namespace                 APP            APP           APP
Default Globals DB        APPDATA        APPDATA       APPDATA
Data Global               ^RAG.ChunkD    ^RAG.ChunkD   ^RAG.ChunkD
Effective Data DB         VECTORDB       VECTORDB      APPDATA      ⚠
Index Global              ^RAG.ChunkI    ^RAG.ChunkI   ^RAG.ChunkI
Effective Index DB        VECTORINDEX    VECTORINDEX   APPDATA      ⚠
Default Routines DB       APPCODE        APPCODE       APP
Effective Code DB         SHARED-CODE    SHARED-CODE   APP          ⚠
```

Interpretation:

```text
DIFFERENT
```

not:

```text
BROKEN
```

Storage drift can be intentional.

---

# 23. DATABASE-LEVEL ACCOUNTING

## 23.1 MVP METRICS

Per logical vector asset:

```text
instance
namespace
schema
table
column
vector type
dimensions
rows with vector
embedding config / recipe
HNSW
effective data database
effective index database
effective code database
mapped/default indicator
```

Per physical database:

```text
INSTANCE DATABASE      VECTOR DATASETS  LOGICAL EXPOSURES  INDEX DATASETS

PROD-01 VECTORDB              4                  7                 0
PROD-01 VECTORINDEX           0                  7                 3
PROD-01 APPDATA               2                  2                 1
```

### RULE

Do NOT claim exact vector-only byte usage unless a tested physical measurement exists.

### FEASIBILITY

**HIGH** for counts and resource association.

**MEDIUM** for precise byte attribution.

---

# 24. GROWTH

## 24.1 PHASE

P2.

The Multi-Manager may collect snapshots:

```text
VectorStorageSnapshot
---------------------
instanceId
namespace
schema
table
column
effectiveDataDatabase
dataGlobal
timestamp
vectorRowCount
databaseMetadata
```

### RULE

No historical chart before the first collected sample.

### SHARED DATASET RULE

If two namespaces resolve to:

```text
same instance
same physical database
same global root
```

the physical dataset is counted once.

### FEASIBILITY

**MEDIUM**

Requires scheduled snapshots and local persistence.

---

# 25. BACKEND DESIGN

## 25.1 PACKAGE

Adapt package names to the existing project root.

Proposed structure:

```text
vector/
├── api/
│   └── VectorResource.java
│
├── service/
│   ├── VectorFleetService.java
│   ├── VectorInventoryService.java
│   ├── VectorModelService.java
│   ├── VectorGenerationService.java
│   ├── VectorIndexService.java
│   ├── VectorExtensionService.java
│   └── VectorLocationService.java
│
├── location/
│   ├── VectorLocationResolver.java
│   ├── NamespaceLocationContext.java
│   ├── GlobalMappingResolver.java
│   ├── CodeMappingResolver.java
│   └── PhysicalDatasetIdentity.java
│
├── jdbc/
│   ├── VectorMetadataRepository.java
│   ├── EmbeddingConfigRepository.java
│   └── VectorSqlProcClient.java
│
├── model/
│   ├── VectorAsset.java
│   ├── EmbeddingConfigView.java
│   ├── ModelCapability.java
│   ├── VectorIndex.java
│   ├── EmbeddingRecipe.java
│   ├── VectorLocation.java
│   ├── VectorStorageSnapshot.java
│   └── VectorOperationResult.java
│
└── security/
    └── EmbeddingConfigSanitizer.java
```

---

# 26. MULTI-MANAGER REST API

Proposed endpoints:

```text
GET  /api/vector/assets
GET  /api/vector/assets/{assetId}
GET  /api/vector/assets/{assetId}/rows

GET  /api/vector/assets/{assetId}/location
GET  /api/vector/assets/{assetId}/location/compare

GET  /api/vector/storage/databases
GET  /api/vector/storage/shared-datasets

GET  /api/vector/models
GET  /api/vector/models/{name}/compare
POST /api/vector/models/{name}/check
POST /api/vector/models/{name}/download
POST /api/vector/models/{name}/test

GET  /api/vector/indexes
POST /api/vector/indexes/preview
POST /api/vector/indexes/apply

GET  /api/vector/recipes
POST /api/vector/regeneration/preview
POST /api/vector/regeneration/apply

GET  /api/vector/extension/status
```

All multi-instance responses SHALL use the existing result semantics:

```text
SUCCESS
FAILED
UNAVAILABLE
SKIPPED
```

---

# 27. FRONTEND DESIGN

Proposed Angular feature:

```text
features/vector/
├── vector-workspace
├── vector-inventory
├── vector-models
├── vector-generation
├── vector-indexes
├── vector-asset-detail
├── vector-location
├── vector-storage-compare
└── vector-data-explorer
```

Reuse existing:

```text
InstanceSelector
FleetOperationResult
Resource Matrix pattern
status badges
preview/apply pattern
```

---

# 28. DEMO CONFIGURATION

## 28.1 PROD-01

```text
RAG.Chunk.Embedding
Type: EMBEDDING
Config: miniLM
Dimension: 384
HNSW: M=16 / efConstruction=64

Namespace: APP
Default Globals DB: APPDATA
Default Routines DB: APPCODE

Data global:
^RAG.ChunkD
→ VECTORDB

Index global:
^RAG.ChunkI
→ VECTORINDEX

Package:
RAG
→ SHARED-CODE

Extension: installed
SentenceTransformers: installed
```

---

## 28.2 PROD-02

```text
Same logical resource

SentenceTransformers:
package or model missing
```

Purpose:

Demonstrate model readiness drift.

---

## 28.3 PROD-03

```text
RAG.Chunk.Embedding
Type: VECTOR
Recipe: chunk-minilm-v2
Dimension: 384
HNSW: missing

Namespace: APP
Default Globals DB: APPDATA
Default Routines DB: APP

No mapping for:
^RAG.ChunkD

No mapping for:
^RAG.ChunkI

No package mapping for RAG
```

Purpose:

Demonstrate:

- native EMBEDDING vs custom VECTOR;
- generation recipe;
- HNSW drift;
- storage-location drift.

---

# 29. DEMO EXECUTION

1. Open Vector Search.
2. Select all three instances.
3. Show vector asset inventory.
4. Select `RAG.Chunk.Embedding`.
5. Compare:
   - type;
   - dimensions;
   - model;
   - HNSW.
6. Open Location.
7. Show:
   - namespace;
   - default globals DB;
   - data global;
   - global mapping;
   - effective data DB;
   - index DB;
   - code DB.
8. Show APP and REPORTING resolving to one physical vector dataset.
9. Open Models.
10. Show PROD-02 model readiness failure.
11. Execute `Check model`.
12. Download/cache model if enabled in demo.
13. Execute test embedding.
14. Open PROD-03 custom VECTOR.
15. Show generation recipe.
16. Regenerate one selected record.
17. Create HNSW with preview.
18. Refresh inventory.
19. Confirm final state per instance.

---

# 30. DOCKER

## 30.1 IRIS IMAGE

Add required pinned Python packages to:

```text
docker/iris/Dockerfile
```

Example:

```text
sentence-transformers==<validated-version>
```

Install using the location/mechanism supported by Embedded Python documentation:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GEPYTHON_loadlib

---

## 30.2 SERVER EXTENSION

Add:

```text
docker/iris/src/MultiManager/Vector/Extension.cls
docker/iris/src/MultiManager/Vector/Recipe.cls
docker/iris/src/MultiManager/Vector/Demo.cls
```

Use the existing demo loader.

### FEASIBILITY

**HIGH**

No remote deployment mechanism is required for the contest demo.

---

## 30.3 DEMO MAPPINGS

Configure at least:

```text
PROD-01 / APP
Globals  = APPDATA
Routines = APPCODE

^RAG.ChunkD → VECTORDB
^RAG.ChunkI → VECTORINDEX
RAG package → SHARED-CODE
```

Also configure:

```text
REPORTING
```

to expose the same physical data global.

Purpose:

Demonstrate:

```text
2 logical namespace exposures
1 physical vector dataset
```

---

# 31. OPTIONAL REMOTE EXTENSION DEPLOYMENT

## 31.1 PHASE

P2 only.

Do NOT block MVP on remote code deployment.

Potential official mechanisms:

- InterSystems Native SDK for Java;
- `%SYSTEM.OBJ` loading/compilation APIs.

References:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=PAGE_java_native

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25SYSTEM.OBJ

### REQUIREMENT

Only deploy:

- known artifact;
- versioned artifact;
- checksum-verified artifact;
- Multi-Manager-owned extension.

### PROHIBITED

```text
browser textarea
    ↓
arbitrary Python
    ↓
execute on production
```

---

# 32. SECURITY

1. Never execute arbitrary Python supplied by the browser.
2. Never return embedding API keys to Angular.
3. Never log secrets.
4. Never download a model without explicit confirmation.
5. Never regenerate all vectors without preview/confirmation.
6. Never drop HNSW without confirmation.
7. Never assume distributed atomicity across instances.
8. Always identify target as `(instance, namespace)`.
9. Always display instance name for destructive operations.
10. Keep Fleet Query read-only.
11. Validate all SQL identifiers against discovered metadata.
12. Never construct unrestricted DDL from raw browser strings.

---

# 33. FAILURE MODEL

The module is multi-instance.

A failed instance SHALL NOT cause successful data from other instances to disappear.

Example:

```json
{
  "instances": [
    {
      "instance": "PROD-01",
      "status": "SUCCESS",
      "data": {}
    },
    {
      "instance": "PROD-02",
      "status": "FAILED",
      "error": "Model cache unavailable"
    },
    {
      "instance": "PROD-03",
      "status": "SUCCESS",
      "data": {}
    }
  ]
}
```

---

# 34. TECHNICAL RISKS

| Risk | Impact | Mitigation |
|---|---|---|
| HNSW metadata shape differs from assumption | medium | validate query against IRIS 2026.2 before DTO freeze |
| Custom storage cannot be fully resolved | medium | return `UNKNOWN`; do not infer |
| Large vector tables make row counts expensive | medium | timeout, optional sampling/count strategy |
| Model download takes long | medium | explicit action, status, timeout |
| Missing `sentence_transformers` | low | capability check |
| Different permissions per namespace | medium | per-instance/per-namespace result |
| API key leakage from `%Embedding.Config` | high | server-side sanitizer |
| Regeneration writes too much data | high | selected IDs first, batching, confirmation |
| HNSW build impacts workload | high | preview + warning + explicit confirmation |
| Shared physical data double-counted | medium | physical identity tuple |
| Mapping precedence misunderstood | high | resolver tests with known demo mappings |

---

# 35. IMPLEMENTATION PHASES

## PHASE 1 — INVENTORY

Use:

```text
SysAdmin:
  /v2/namespaces
  /v2/databases

JDBC:
  INFORMATION_SCHEMA.COLUMNS
  %Embedding.Config
```

Deliver:

- Vector workspace;
- vector inventory;
- dimensions;
- model/config;
- row counts.

**GO / NO-GO:** assets from three instances appear on one screen.

---

## PHASE 2 — LOCATION RESOLUTION

Use:

```text
SysAdmin:
  /v2/namespaces
  /v2/namespace/global-mappings
  /v2/namespace/routine-mappings
  /v2/namespace/package-mappings

JDBC / IRIS metadata:
  persistent class storage definition
```

Deliver:

- effective data DB;
- effective index DB;
- effective code DB;
- shared physical dataset detection;
- Storage Compare.

**GO / NO-GO:** UI correctly explains `namespace → global → physical DB`.

---

## PHASE 3 — MODEL CONTROL

Use:

```text
%Embedding.Config
SqlProc
Embedded Python
%Embedding.SentenceTransformers
```

Deliver:

- extension status;
- model readiness;
- test embedding;
- optional model download.

**GO / NO-GO:** model drift can be detected across instances.

---

## PHASE 4 — GENERATION

Use:

```text
EmbeddingRecipe
SqlProc
Embedded Python
JDBC writes
```

Deliver:

- recipe association for custom VECTOR;
- test one record;
- regenerate selected IDs.

**GO / NO-GO:** one selected vector can be regenerated and verified.

---

## PHASE 5 — HNSW

Use:

```text
INFORMATION_SCHEMA.INDEXES
%Dictionary.CompiledIndex
CREATE INDEX ... AS HNSW
BUILD INDEX
DROP INDEX
```

Deliver:

- HNSW inventory;
- compare;
- create preview/apply;
- rebuild;
- drop.

**GO / NO-GO:** index state is refreshed and visible after action.

---

## PHASE 6 — DPI-I-557 ACCEPTANCE

Deliver:

- source data explorer;
- vector provenance;
- model/config visibility;
- generation visibility;
- HNSW management;
- multi-instance comparison;
- storage location.

---

# 36. DEFINITION OF DONE

The feature is complete when:

- [ ] Vector Search is a workspace inside IRIS Multi-Manager.
- [ ] Three IRIS instances can be compared.
- [ ] `VECTOR` and `EMBEDDING` assets are discovered.
- [ ] `%Embedding.Config` is visible per instance.
- [ ] secrets are sanitized.
- [ ] SentenceTransformers readiness is visible.
- [ ] custom project code uses Embedded Python.
- [ ] Embedded Python is exposed through `SqlProc`.
- [ ] Quarkus invokes the procedure through JDBC.
- [ ] custom `VECTOR` assets can have a generation recipe.
- [ ] one selected embedding can be regenerated.
- [ ] HNSW is inventoried.
- [ ] HNSW can be created, rebuilt and dropped with confirmation.
- [ ] source data is available through a limited explorer.
- [ ] namespace defaults come from `mainspec_v2.json`.
- [ ] global mappings resolve Effective Data DB.
- [ ] routine/package mappings resolve Effective Code DB when applicable.
- [ ] DataLocation and IndexLocation are displayed when resolvable.
- [ ] data and index can resolve to different databases.
- [ ] shared physical datasets are not double-counted within one instance.
- [ ] unknown storage is shown as `UNKNOWN`.
- [ ] vector functionality is not falsely attributed to SysAdmin API.
- [ ] partial failure is reported per instance.
- [ ] Docker demo starts from a clean environment.
- [ ] all relevant docs are linked in README/SPEC.

---

# 37. OFFICIAL REFERENCES

## SysAdmin API

Specification:

https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json

Relevant operations:

```text
/info

/v2/databases
/v2/database
/v2/database-dir

/v2/namespaces

/v2/namespace/global-mappings
/v2/namespace/global-mapping

/v2/namespace/routine-mappings
/v2/namespace/routine-mapping

/v2/namespace/package-mappings
/v2/namespace/package-mapping
```

---

## Vector Search

Using Vector Search:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GSQL_vecsearch

SQL VECTOR datatype:

https://docs.intersystems.com/irislatest/csp/docbook/Doc.View.cls?KEY=RSQL_datatype

Embedding configuration:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25Embedding.Config

SentenceTransformers:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25Embedding.SentenceTransformers&LIBRARY=%25SYS

---

## SQL Metadata

Columns:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=INFORMATION.SCHEMA.COLUMNS&LIBRARY=%25SYS

Indexes:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=INFORMATION.SCHEMA.INDEXES

Compiled index metadata:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25Dictionary.CompiledIndex&LIBRARY=%25SYS

---

## Storage / Namespace

CreateNamespace:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=RACS_createnamespace

Storage Definitions:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GOBJ_storage

Persistent Objects and SQL:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GORIENT_persistence

Namespace mappings:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GSA_config_namespace_addmap

Global mapping:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GGBL_mapping

---

## Embedded Python

Embedded Python:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GEPYTHON

Install Python packages:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GEPYTHON_loadlib

Method `Language` keyword:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ROBJ_method_language

---

## Stored Procedures / Java

`SqlProc`:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ROBJ_method_sqlproc

JDBC CallableStatement:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25XDBC.Gateway.JDBC.CallableStatement&LIBRARY=%25SYS

Native SDK for Java:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=PAGE_java_native

`%SYSTEM.OBJ`:

https://docs.intersystems.com/irislatest/csp/documatic/%25CSP.Documatic.cls?CLASSNAME=%25SYSTEM.OBJ

---

## HNSW

Vector Search / HNSW:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GSQL_vecsearch

BUILD INDEX:

https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=RSQL_buildindex

---

## Community Opportunity

DPI-I-557:

https://ideas.intersystems.com/ideas/DPI-I-557

---

# 38. FINAL ARCHITECTURE

```text
                              ANGULAR
                                 │
                              QUARKUS
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
        SysAdmin API            JDBC            JDBC CALL
              │                  │                  │
    namespace/database      Vector SQL            SqlProc
       /mappings            metadata               │
              │             HNSW                   ▼
              │        %Embedding.Config      Embedded Python
              │                  │             SentenceTransformers
              └──────────────────┴──────────────────┘
                                 │
                                IRIS
                ┌────────────────┼────────────────┐
                │                │                │
          Namespace/DB         VECTOR          Python Model
          Storage Model        HNSW            Execution
```

---

# 39. FINAL TECHNICAL DECISION

**KEEP QUARKUS.**

**DO NOT migrate the backend to Python.**

Use:

```text
SysAdmin API
```

for administrative capabilities explicitly present in `mainspec_v2.json`.

Use:

```text
JDBC
```

for Vector Search SQL and metadata.

Use:

```text
SqlProc + Embedded Python
```

for local model/embedding operations that must execute inside IRIS.

Use:

```text
Java resolver services
```

to combine namespace defaults, mappings and storage definitions into one location model.

Central rule:

> **Java controls the fleet. SysAdmin API supplies official administrative state. JDBC supplies SQL/vector capabilities. Embedded Python performs controlled local model operations inside IRIS.**
