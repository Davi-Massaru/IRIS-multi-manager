# IRIS Multi-Manager

IRIS Multi-Manager is a fleet-first administration console for independent InterSystems IRIS Community Edition servers. It keeps the owning instance visible for every result and reports partial failures per target.

## Architecture

```text
Browser → Angular/Nginx → Quarkus → SysAdmin REST → iris-prod1
                                      ├──────────→ iris-prod2
                                      └──────────→ iris-prod3
                                  JDBC is used only by Fleet Query
```

The demo has five services: `frontend`, `backend`, and three independent IRIS 2026.2 Community Edition instances. Each IRIS target has its own durable volume and Docker DNS name. No application agent is installed in the managed instances; only small demo fixtures are loaded.

## Run

Requirements: Docker Desktop with at least 8 GB available memory.

```powershell
./scripts/initialize-demo.ps1
docker compose config
docker compose build
docker compose up -d
docker compose ps
```

## Links e acesso do ambiente de demonstração

| Aplicação | Link local | Usuário | Senha |
| --- | --- | --- | --- |
| IRIS Multi-Manager | [http://localhost:8080](http://localhost:8080) | `_SYSTEM` para conectar às instâncias | Senha gerada do IRIS |
| Portal de Gerenciamento PROD-01 | [http://localhost:52773/csp/sys/UtilHome.csp](http://localhost:52773/csp/sys/UtilHome.csp) | `_SYSTEM` | Senha gerada do IRIS |
| Portal de Gerenciamento PROD-02 | [http://localhost:52774/csp/sys/UtilHome.csp](http://localhost:52774/csp/sys/UtilHome.csp) | `_SYSTEM` | Senha gerada do IRIS |
| Portal de Gerenciamento PROD-03 | [http://localhost:52775/csp/sys/UtilHome.csp](http://localhost:52775/csp/sys/UtilHome.csp) | `_SYSTEM` | Senha gerada do IRIS |

**Não há senha padrão fixa neste projeto.** `./scripts/initialize-demo.ps1` gera uma senha aleatória uma única vez e a salva em `.runtime/iris-password`, arquivo ignorado pelo Git. As três instâncias de demonstração recebem essa mesma senha para `_SYSTEM`. Para consultá-la localmente, na raiz do projeto, execute:

```powershell
Get-Content .runtime/iris-password
```

No Multi-Manager, abra a aba **Instances**, selecione as instâncias, informe `_SYSTEM` e a senha acima e clique em **Connect**. O Multi-Manager não possui uma conta ou senha própria: ele autentica em cada IRIS selecionado. O backend em `http://localhost:8081` é uma API, sem tela de login separada.

As portas locais de SuperServer/JDBC das três instâncias são `1972`, `1973` e `1974`, respectivamente. Em uma instalação fora do ambiente de demonstração, use as credenciais reais de cada servidor; elas podem ser diferentes.

## Adicionar um servidor

O cadastro de instâncias é feito na configuração do backend, sem formulário na interface. Adicione o host a `fleet.allowed-hosts` e um objeto com `id`, `name`, `adminBaseUrl`, `environment`, `jdbcHost`, `jdbcPort`, `defaultNamespace` e `enabled` a `fleet.instances` em `backend/src/main/resources/application.properties`. O `adminBaseUrl` deve terminar em `/api/admin`, e o backend precisa alcançar esse host e a porta JDBC pela rede. Depois, reconstrua e reinicie o backend com `docker compose up -d --build backend` e atualize a página. Se o novo IRIS for outro serviço do Compose, conecte-o à rede `iris-multi-manager-net`. Cada servidor usa suas próprias credenciais na sessão; a senha de demonstração não é aplicada a servidores externos.

## Features

- independent session-scoped authentication for each configured target;
- global process search, detail and target-confirmed suspend/resume/terminate;
- reusable presence and configuration matrices for Web Apps, Tasks, Users, Roles, Resources, Wallet collections, X509 credentials and OAuth metadata;
- preflight, preview, explicit confirmation, bounded execution and post-write verification for Web App and Task changes;
- system dashboard and audit event fanout with instance attribution;
- read-only JDBC Fleet Query with a single SELECT, per-target timeout, row limit and grouped results;
- offline target isolation: one IRIS instance can be stopped without collapsing the fleet view.

Passwords remain in the Quarkus session memory and are never returned to Angular, persisted in browser storage, or written to logs. Secret-bearing wallet values are never selected for response matrices.

## Tests

The backend container runs the unit suite during its Maven build. The suite covers bounded fleet execution, timeouts, partial failure, process identity, URL allowlisting, credential redaction, resource safety and SELECT-only enforcement.

Run live checks after startup:

```powershell
./scripts/smoke-process.ps1
./scripts/smoke-webapps.ps1
./scripts/smoke-tasks.ps1
./scripts/smoke-security.ps1
./scripts/smoke-secrets.ps1
./scripts/smoke-system-events.ps1
./scripts/smoke-query.ps1
```

The operations are intentionally not distributed transactions. A successful target remains successful if another target fails; the result reports each target independently.
