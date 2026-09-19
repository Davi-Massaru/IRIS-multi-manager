# IRIS Multi-Manager

IRIS Multi-Manager is a web console for administrators who work with more than one independent InterSystems IRIS instance. It brings fleet-wide visibility and selected management actions into one workspace. The project was built for the [InterSystems Programming Contest: Build Your Own Management Portal](https://community.intersystems.com/post/intersystems-programming-contest-build-your-own-management-portal).

## Why it exists

The standard Management Portal shows one IRIS instance at a time. When an administrator needs to find a process, compare a task or web application, or repeat a change on several servers, moving between portals makes it hard to see differences and partial failures. IRIS Multi-Manager keeps the owning instance visible in every result. Its central questions are: **Where is it? Where is it missing? What differs? What happened on each selected server?**

The application connects to the [SysAdmin API](https://github.com/intersystems-community/sysadmin-api-specification) on each IRIS instance. A Quarkus backend holds each server's credentials in its session, performs bounded calls, and returns separate outcomes per instance. An Angular frontend presents the results. Fleet Query uses JDBC for read-only SQL queries.

## What you can do

- Search processes across connected servers, inspect a process by instance and PID, and confirm suspend, resume, or terminate actions on that specific process.
- Compare the presence and selected configuration of web applications, tasks, users, roles, resources, wallet collections, X509 credentials, and OAuth metadata.
- Preview and apply supported web application and task changes to selected servers, then inspect the result for each server.
- View system information and audit events per instance.
- Run one bounded, read-only SELECT query on selected instances and view rows grouped by server.

A failed or offline server does not hide successful results from the others. Operations across servers are not distributed transactions.

## Run the three-server demo

You need Docker Desktop with Docker Compose and at least 8 GB of available memory. From the repository root:

```powershell
docker compose up -d --build
docker compose ps
```

The first build downloads the IRIS, Java, and frontend dependencies and can take several minutes. Wait until the three IRIS containers report `healthy`. Then open [http://localhost:8080](http://localhost:8080).

The demo username is **`_SYSTEM`** and the password is **`IrisDemo2026!`** for all three IRIS instances. The password is written directly in `docker-compose.yml` for this local demonstration. It is public and must not be reused for a real server.

| Application | Local address | Login |
| --- | --- | --- |
| IRIS Multi-Manager | [http://localhost:8080](http://localhost:8080) | In **Instances**, connect with `_SYSTEM` / `IrisDemo2026!` |
| PROD-01 Management Portal | [http://localhost:52773/csp/sys/UtilHome.csp](http://localhost:52773/csp/sys/UtilHome.csp) | `_SYSTEM` / `IrisDemo2026!` |
| PROD-02 Management Portal | [http://localhost:52774/csp/sys/UtilHome.csp](http://localhost:52774/csp/sys/UtilHome.csp) | `_SYSTEM` / `IrisDemo2026!` |
| PROD-03 Management Portal | [http://localhost:52775/csp/sys/UtilHome.csp](http://localhost:52775/csp/sys/UtilHome.csp) | `_SYSTEM` / `IrisDemo2026!` |

The Multi-Manager has no separate account. Select the three instances on its **Instances** screen, enter the demo credentials, and click **Connect**. Connections are authenticated independently; on other servers, use each server's own credentials. The backend's local port `8081` serves the API, not another login page.

The three demo IRIS servers have separate persistent volumes. Their local JDBC ports are `1972`, `1973`, and `1974`. If you used an earlier version with a generated password, recreate the containers with `docker compose up -d --build` to apply the fixed demo password. The persistent volumes are retained.

## Try the application

1. Open **Instances**, select PROD-01, PROD-02, and PROD-03, and connect.
2. Open **Processes** and search for `DemoWorker`. The results show which server owns each process. Open **Details** to see one process; actions require an explicit confirmation that includes its server and PID.
3. Open **Web Apps** or **Tasks** to compare presence and configuration. The demo deliberately includes resources that exist on only some servers. Review a preview before applying a supported change.
4. Open **Permissions**, **System**, and **Events** to inspect each server's security metadata, system data, and audit records.
5. Open **Fleet Query** and run a SELECT query. The results remain grouped by IRIS instance.

## Add another IRIS connection

Connections are currently registered in the backend configuration; there is no **Add server** form in the UI. The backend must be able to reach the server's SysAdmin API. The registry also requires a JDBC host and port for Fleet Query.

1. Choose a hostname reachable **from the backend container**. For an IRIS service in this Compose project, use its Compose service name on `iris-multi-manager-net`. For a server running on the Docker host, `host.docker.internal` is usually the reachable hostname on Docker Desktop. A remote server needs a DNS name or IP reachable from the container.
2. Edit [`backend/src/main/resources/application.properties`](backend/src/main/resources/application.properties). Add that exact hostname to `fleet.allowed-hosts`.
3. Append a JSON object to the existing `fleet.instances` array in the same file. Use a unique lowercase `id`, a display `name`, and an `adminBaseUrl` ending in `/api/admin`. Set `jdbcHost`, `jdbcPort`, and `defaultNamespace` for Fleet Query. For example, for a fourth IRIS reachable as `iris-prod4`, add `iris-prod4` to the allowlist and append this object after the existing PROD-03 object:

   ```properties
   fleet.allowed-hosts=iris-prod1,iris-prod2,iris-prod3,iris-prod4
   ```

   ```json
   {"id":"prod4","name":"PROD-04","adminBaseUrl":"http://iris-prod4:52773/api/admin","environment":"DEMO","jdbcHost":"iris-prod4","jdbcPort":1972,"defaultNamespace":"USER","enabled":true}
   ```

   Keep the existing objects and put a comma before the new object. Keep the entire `fleet.instances` property value as a valid JSON array on one line. Both the SysAdmin and JDBC hostnames must be on the allowlist.
4. Rebuild the backend and refresh the browser:

   ```powershell
   docker compose up -d --build backend
   ```

5. Select the new instance in **Instances** and connect using an account on that IRIS server. The demo password applies only to the three bundled demo instances.

You can disable an entry by setting `enabled` to `false` and rebuilding the backend. Registry changes require a restart; credentials are entered at connection time and are not stored in `application.properties`.

## Validate and stop

After all services are healthy, the PowerShell smoke scripts exercise the live demo:

```powershell
./scripts/smoke.ps1
./scripts/smoke-process.ps1
./scripts/smoke-webapps.ps1
./scripts/smoke-tasks.ps1
./scripts/smoke-security.ps1
./scripts/smoke-secrets.ps1
./scripts/smoke-system-events.ps1
./scripts/smoke-query.ps1
```

The backend unit suite runs during its Docker build. To stop the application while retaining demo data, run `docker compose down`. The three named volumes remain until explicitly removed.

## Architecture at a glance

```text
Browser → Angular / Nginx → Quarkus → SysAdmin API → IRIS PROD-01
                                      ├───────────→ IRIS PROD-02
                                      └───────────→ IRIS PROD-03
                              JDBC → selected IRIS instances (Fleet Query)
```

This project is a multi-instance administration client. It does not install an application agent on the managed servers; the bundled IRIS containers contain only small demo fixtures. The backend uses Basic authentication to the SysAdmin API, keeps credentials in its server-side session, and does not return passwords or secret values to the browser. The demo publishes ports only on localhost. Review authentication, transport security, and access policy before connecting to non-demo systems.
