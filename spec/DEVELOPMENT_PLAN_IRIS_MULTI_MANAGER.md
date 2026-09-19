# IRIS Multi-Manager — Plano de Desenvolvimento

**Status:** PLANO DE EXECUÇÃO DO MVP  
**Base obrigatória:** `intersystems-community/intersystems-iris-dev-template`  
**Fonte de requisitos:** `SPEC_IRIS_Multi_Manager.md`  
**Regra de engenharia:** usar somente tecnologias justificadas pela SPEC.

---

# 0. MISSÃO

Construir o **IRIS Multi-Manager** como console externo, fleet-first, capaz de administrar múltiplas instâncias InterSystems IRIS independentes.

ENTREGAR:

1. uma interface Angular única;
2. um backend Quarkus único;
3. três instâncias IRIS Community Edition independentes para demonstração;
4. integração administrativa prioritariamente pela **SysAdmin REST API**;
5. JDBC apenas para **Fleet Query**;
6. operação parcial tolerante a falhas por instância;
7. ambiente completo reproduzível por Docker Compose.

NÃO ENTREGAR NO MVP:

- Python;
- Embedded Python;
- Flask;
- WSGI;
- ODBC;
- ECP;
- RAG/LLM/IA;
- Kafka;
- Redis;
- PostgreSQL;
- transação distribuída;
- agente instalado em cada IRIS administrado.

**Motivo:** nenhuma dessas tecnologias é necessária para cumprir a SPEC atual. A aplicação é explicitamente um cliente externo multi-instância. Adicionar tecnologia sem responsabilidade concreta viola a regra de stack mínima do `IRIS.agent.md`.

**REFERÊNCIAS**

- Agent: decisão external vs IRIS-hosted e stack mínima.
- SPEC: arquitetura Angular → Quarkus → múltiplas instâncias IRIS; JDBC somente no Fleet Query.
- SysAdmin API v2: base URL `/api/admin`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L1-L13
- Concurso: GUI deve usar APIs de gerenciamento do IRIS e funcionar com Community Edition:  
  https://community.intersystems.com/post/intersystems-programming-contest-build-your-own-management-portal

---

# 1. CONCEITO DE OPERAÇÃO

## 1.1 Caminho principal

```text
BROWSER
   |
   v
ANGULAR / NGINX
   |
   | /api/*
   v
QUARKUS
   |
   +---------------- SysAdmin REST ----------------+
   |                    |                          |
   v                    v                          v
IRIS PROD-01         IRIS PROD-02              IRIS PROD-03
:52773               :52773                    :52773
/api/admin            /api/admin                 /api/admin
```

## 1.2 Caminho excepcional — Fleet Query

```text
QUARKUS
   |
   +---------------- JDBC -------------------------+
   |                    |                          |
   v                    v                          v
IRIS PROD-01         IRIS PROD-02              IRIS PROD-03
:1972                :1972                     :1972
```

REGRA:

- usar REST SysAdmin para administração;
- usar JDBC somente para SQL fleet query;
- não usar JDBC como atalho para reimplementar funções já expostas pela SysAdmin API;
- não permitir acesso Angular → IRIS direto.

**REFERÊNCIAS**

- SysAdmin API: OpenAPI v3, API v2, `/api/admin`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L1-L13
- JDBC InterSystems: driver Type 4 JDBC 4.2:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=BTPI_jdbc
- Formato de conexão JDBC `jdbc:IRIS://host:port/namespace`:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=AFL_jdbc

---

# 2. LIMPEZA DO TEMPLATE — EXECUTAR PRIMEIRO

OBJETIVO: transformar o template em repositório do produto antes de iniciar feature.

EXECUTAR:

1. remover `src/dc/sample`;
2. remover `tests/dc/sample/unittests`;
3. remover referências a `dc-sample`;
4. remover classes persistentes demonstrativas;
5. remover testes demonstrativos;
6. remover `requirements.txt` caso não exista dependência Python real;
7. remover configuração de Embedded Python do Dockerfile;
8. remover `PYTHON_PATH`, `IRISUSERNAME`, `IRISPASSWORD`, `IRISNAMESPACE` quando existirem apenas para o exemplo Python;
9. remover configuração de `%Service_CallIn` caso não seja necessária;
10. remover Dockerfiles alternativos não utilizados (`Dockerfile_less_image`, `Dockerfile_mini`) se não tiverem papel no projeto;
11. revisar `.github/workflows`; manter apenas workflows que serão usados;
12. substituir README do template;
13. substituir nome/package do `module.xml` somente se IPM continuar sendo usado para os artefatos demo IRIS;
14. procurar resíduos:

```bash
grep -R "dc.sample\|dc-sample\|PackageSample" . --exclude-dir=.git
```

DECISÃO:

- o produto principal **não depende de ZPM/IPM** para executar Angular/Quarkus;
- IPM poderá permanecer somente se for usado para empacotar classes ObjectScript de **fixture/demo** instaladas nos três IRIS;
- se nenhum fixture exigir IPM, remover `module.xml` e fluxo ZPM do produto.

GATE 2:

- nenhum identificador de exemplo permanece;
- `docker compose config` ainda é válido após reorganização inicial;
- repositório contém somente infraestrutura útil ao produto.

**REFERÊNCIAS**

- Template oficial: possui `src/dc/sample`, testes, Dockerfile, `iris.script`, `merge.cpf`, `module.xml` e `requirements.txt`:  
  https://github.com/intersystems-community/intersystems-iris-dev-template
- Dockerfile do template: carrega merge CPF, `iris.script` e ZPM durante build:  
  https://github.com/intersystems-community/intersystems-iris-dev-template/blob/master/Dockerfile
- `iris.script` do template: contém preparação ZPM e configuração de desenvolvimento:  
  https://github.com/intersystems-community/intersystems-iris-dev-template/blob/master/iris.script
- `module.xml` de exemplo `dc-sample`:  
  https://github.com/intersystems-community/intersystems-iris-dev-template/blob/master/module.xml

---

# 3. ESTRUTURA FINAL DO REPOSITÓRIO

ADOTAR:

```text
iris-multi-manager/
|
|-- frontend/
|   |-- src/
|   |-- nginx.conf
|   |-- Dockerfile
|   `-- package.json
|
|-- backend/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- src/test/java/
|   |-- pom.xml
|   `-- Dockerfile
|
|-- docker/
|   `-- iris/
|       |-- Dockerfile
|       |-- merge.cpf
|       |-- iris.script
|       |-- src/
|       |   `-- MultiManager/Demo/
|       |-- profiles/
|       |   |-- dev/
|       |   |-- qa/
|       |   `-- prod/
|       `-- README.md
|
|-- docs/
|   |-- architecture.md
|   |-- sysadmin-api-coverage.md
|   `-- demo-script.md
|
|-- docker-compose.yml
|-- .env.example
|-- SPEC.md
|-- AGENT.md
|-- DEVELOPMENT_PLAN.md
`-- README.md
```

REGRA:

- `docker/iris/src` contém somente fixtures necessárias à demonstração;
- lógica real de fleet permanece no Quarkus;
- nenhum código da aplicação Multi-Manager deve precisar ser instalado em instâncias IRIS reais do usuário.

**REFERÊNCIAS**

- O template permite substituir o package de exemplo por package real:  
  https://github.com/intersystems-community/intersystems-iris-dev-template
- Containers InterSystems separam código/dados e suportam CI/CD/DevOps:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=PAGE_deployment_containers

---

# 4. ORDEM DE BATALHA DO DOCKER

## 4.1 Serviços obrigatórios

CRIAR SOMENTE:

```text
frontend
backend
iris-prod1
iris-prod2
iris-prod3
```

NÃO CRIAR:

```text
postgres
redis
python
flask
wsgi
gunicorn
odbc-gateway
agent-per-iris
```

## 4.2 Rede

CRIAR:

```text
iris-multi-manager-net
```

REGRAS:

- todos os serviços entram na mesma rede privada do Compose;
- comunicação interna usa DNS de serviço Docker;
- jamais usar `localhost` para outro container;
- frontend nunca chama `iris-prod1`, `iris-prod2` ou `iris-prod3` diretamente.

## 4.3 Portas

```text
frontend    container 80      host 8080
backend     container 8080    host 8081 somente para debug/demo
iris-prod1  container 52773   host 52773
iris-prod1  container 1972    host 1972
iris-prod2  container 52773   host 52774
iris-prod2  container 1972    host 1973
iris-prod3  container 52773   host 52775
iris-prod3  container 1972    host 1974
```

DENTRO DA REDE:

```text
http://iris-prod1:52773/api/admin
http://iris-prod2:52773/api/admin
http://iris-prod3:52773/api/admin

jdbc:IRIS://iris-prod1:1972/USER
jdbc:IRIS://iris-prod2:1972/USER
jdbc:IRIS://iris-prod3:1972/USER
```

## 4.4 Volumes

CRIAR volumes independentes:

```text
iris_prod1_data
iris_prod2_data
iris_prod3_data
```

REGRA:

- nunca compartilhar durable `%SYS` entre instâncias;
- cada container representa um servidor IRIS independente;
- documentar reset total de demonstração:

```bash
docker compose down -v --remove-orphans
```

## 4.5 Health checks

IMPLEMENTAR:

- IRIS: validar que a instância está iniciada;
- backend: endpoint health do Quarkus;
- frontend: HTTP 200 do Nginx.

REGRA:

- backend deve iniciar mesmo que um IRIS esteja offline;
- uma instância indisponível é cenário funcional do produto, não falha fatal do backend.

## 4.6 Base IRIS

USAR uma tag Community Edition compatível com a versão da SysAdmin API alvo.

NÃO usar `latest` silenciosamente na versão final do concurso.

FIXAR a tag antes do release.

NOTA:

- `/login` JWT é documentado no `mainspec_v2.json` como disponível a partir do IRIS 2026.2;
- Basic Auth continua sendo o baseline de compatibilidade da SPEC.

**REFERÊNCIAS**

- Community Edition em container e portas 1972/52773:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ACLOUD
- Docker Compose para InterSystems IRIS:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ADOCK
- Durable `%SYS` e `ISC_DATA_DIRECTORY`:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ADOCK
- Configuration Merge / `ISC_CPF_MERGE_FILE`:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GCMF_deploy
- `/info` e `/login`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L13-L143

---

# 5. IMAGEM IRIS DE DEMONSTRAÇÃO

OBJETIVO: executar três IRIS reais com diferenças controladas.

ESTRATÉGIA:

1. usar um único `docker/iris/Dockerfile`;
2. receber `ARG DEMO_PROFILE=PROD1|PROD2|PROD3`;
3. instalar somente fixtures necessários;
4. iniciar IRIS temporariamente durante build;
5. aplicar `merge.cpf` quando necessário;
6. carregar classes de fixture;
7. executar seed do perfil;
8. parar IRIS corretamente;
9. reutilizar camadas comuns entre os três builds.

PERFIS:

```text
PROD-01
- /api/demo
- /api/internal
- DemoCleanup
- DemoWorker

PROD-02
- /api/demo
- /api/internal
- DemoCleanup
- DemoAudit
- DemoWorker

PROD-03
- /api/demo
- /api/internal AUSENTE
- DemoCleanup
- DemoWorker ou processo controlado equivalente
```

IMPLEMENTAR fixture mínima em ObjectScript somente se necessária para:

- registrar Web Applications demo;
- criar tarefas demo;
- iniciar processo longo/identificável `DemoWorker`;
- criar tabela pequena para Fleet Query.

NÃO COLOCAR:

- lógica fleet;
- lógica Angular;
- lógica Quarkus;
- Python;
- WSGI.

GATE 5:

- três containers iniciam;
- os três respondem individualmente;
- diferenças PROD-01/PROD-02/PROD-03 são verificáveis pelo Management Portal/SysAdmin API.

**REFERÊNCIAS**

- Fluxo de build do template (`iris start` → merge → script → stop):  
  https://github.com/intersystems-community/intersystems-iris-dev-template/blob/master/Dockerfile
- Configuration Merge para containers:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=GCMF_deploy
- Web Applications SysAdmin API:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L14627-L15095
- Task APIs:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L13884-L14180

---

# 6. BACKEND — FUNDAÇÃO QUARKUS

OBJETIVO: estabelecer o único ponto de integração com as instâncias IRIS.

CRIAR PACOTES:

```text
connection/
  IrisInstance
  InstanceRegistry
  InstanceAuthenticationService
  CredentialContext
  InstanceEndpointPolicy

sysadmin/
  SysAdminClient
  SysAdminClientFactory
  dto/

fleet/
  FleetExecutor
  FleetResult
  FleetTargetResult
  FleetError
  PreflightService
```

## 6.1 IrisInstance

CAMPOS MÍNIMOS:

```text
id
name
adminBaseUrl
environment
username
authMode
jdbcHost
jdbcPort
defaultNamespace
enabled
```

NÃO persistir senha junto do objeto.

## 6.2 InstanceRegistry

MVP:

- carregar as três instâncias por configuração Quarkus;
- modelar como coleção dinâmica;
- não codificar regra `size == 3`;
- expor API para listar/validar conexões.

## 6.3 CredentialContext

MVP:

- manter credenciais somente em memória;
- vincular contexto a sessão do Multi-Manager;
- nunca devolver senha ao Angular após autenticação;
- nunca escrever senha/token em log.

## 6.4 InstanceEndpointPolicy

VALIDAR URL antes de qualquer chamada de saída.

REJEITAR:

- protocolos não permitidos;
- hosts fora do registry/allowlist;
- redirects inesperados para host diferente;
- URL arbitrária fornecida diretamente pelo browser.

## 6.5 SysAdminClient

REGRA:

`SysAdminClient` representa UMA instância.

NÃO colocar loop fleet dentro dele.

IMPLEMENTAR primeiro:

```http
GET /info
GET /v2/processes
GET /v2/process?pid=...
```

AUTENTICAÇÃO:

1. Basic Auth primeiro;
2. JWT `/login` depois;
3. refresh/logout somente após o fluxo básico estabilizar.

GATE 6:

- backend consulta `/info` das três instâncias;
- falha de uma instância vira resultado `OFFLINE/TIMEOUT`, não HTTP 500 global;
- Authorization é redigido dos logs.

**REFERÊNCIAS**

- `/info`, `/login`, `/logout`, `/refresh`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L13-L159
- Base URL `/api/admin`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L1-L13
- Process detail:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6169-L6220
- Process list/filter:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6384-L6441

---

# 7. FLEET EXECUTOR — NÚCLEO DO PRODUTO

OBJETIVO: executar a mesma operação em N instâncias sem transformar uma falha local em falha global.

CONTRATO:

```text
execute(targetInstances, operation)
    -> FleetResult<T>
```

`FleetResult`:

```text
requestedTargets
completedTargets
successCount
failureCount
results[]
```

`FleetTargetResult`:

```text
instanceId
instanceName
status
httpStatus
elapsedMs
data
errorCode
message
```

STATUS NORMALIZADO:

```text
SUCCESS
UNAUTHORIZED
FORBIDDEN
NOT_FOUND
TIMEOUT
OFFLINE
UNSUPPORTED
FAILED
SKIPPED
```

EXECUTAR:

1. limite de concorrência configurável;
2. timeout por instância;
3. operação independente por target;
4. captura de erro individual;
5. agregação somente após finalizar/expirar targets;
6. manter `instanceId` e `instanceName` em todos os resultados.

NÃO FAZER:

- `fail-fast` porque IRIS PROD-02 caiu;
- transação distribuída;
- rollback automático cross-instance.

TESTAR OBRIGATORIAMENTE:

- 3/3 sucesso;
- 2 sucesso + 1 offline;
- 1 timeout;
- 1 HTTP 401;
- 1 HTTP 403;
- ordem de retorno diferente da ordem de disparo;
- mesmo PID em duas instâncias.

GATE 7:

- `FleetExecutor` aprovado antes de Web Apps, Tasks e Security;
- todas as próximas features devem reutilizar este núcleo.

**REFERÊNCIAS**

- SPEC estabelece independência das instâncias e resultado parcial.
- SysAdmin API possui códigos 401/403/404/500 nos endpoints de administração; exemplo process detail:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6169-L6220
- Async-result existe na API e pode ser integrado posteriormente caso alguma ação administrativa usada retorne execução assíncrona:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L160-L397

---

# 8. FASE 1 — VERTICAL SLICE: GLOBAL PROCESS EXPLORER

PRIORIDADE: MÁXIMA.

OBJETIVO: provar a tese fleet-first o mais cedo possível.

## 8.1 Backend

IMPLEMENTAR:

```http
GET /api/instances
POST /api/instances/{id}/validate
GET /api/processes?instances=...&filter=...
GET /api/processes/{instanceId}/{pid}
```

Integração IRIS:

```http
GET {instance}/api/admin/info
GET {instance}/api/admin/v2/processes?filter=...
GET {instance}/api/admin/v2/process?pid=...
```

NORMALIZAR identidade:

```text
(instanceId, pid)
```

NUNCA usar PID isolado como chave global.

## 8.2 Frontend

CRIAR:

```text
features/instances/
features/processes/
shared/instance-selector/
shared/fleet-operation-result/
```

TABELA:

```text
INSTANCE | PID | NAMESPACE | ROUTINE | USER | STATE | CPU | CLIENT
```

`INSTANCE` é coluna obrigatória e de primeira classe.

## 8.3 Ações de processo

SOMENTE após leitura/detail funcionar.

IMPLEMENTAR:

```http
POST /api/processes/{instanceId}/{pid}/suspend
POST /api/processes/{instanceId}/{pid}/resume
POST /api/processes/{instanceId}/{pid}/terminate
```

Antes de ação destrutiva:

1. buscar detalhe novamente;
2. confirmar que `(instanceId,pid)` ainda existe;
3. mostrar instância e PID;
4. solicitar confirmação;
5. executar somente naquela instância;
6. retornar instância no resultado.

GATE 8:

- pesquisa única consulta três IRIS;
- mesmo PID em duas instâncias não colide;
- uma instância offline não remove resultados das outras;
- pelo menos uma ação segura é demonstrada no IRIS correto.

**REFERÊNCIAS**

- Listagem de processos + filtro:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6384-L6441
- Detalhe de processo:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6169-L6220
- Resume:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6265-L6297
- Suspend:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6298-L6340
- Terminate:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6341-L6383

---

# 9. FASE 2 — RESOURCE PRESENCE MATRIX + WEB APPLICATIONS

OBJETIVO: criar o componente reutilizável de comparação cross-instance.

## 9.1 Contrato comum

CRIAR modelo:

```text
ResourceFleetRow<K,T>
  resourceKey
  targets[]

ResourceTargetState<T>
  instanceId
  presence
  enabled
  normalizedValue
  diffState
```

ESTADOS:

```text
PRESENT
MISSING
DIFFERENT
OFFLINE
FORBIDDEN
UNSUPPORTED
```

## 9.2 Web Apps

CONSUMIR:

```http
GET    /v2/web-apps
GET    /v2/web-app?name=...
PUT    /v2/web-app?name=...
DELETE /v2/web-app?name=...
```

IMPLEMENTAR primeiro:

1. listagem fleet;
2. matriz presença;
3. detalhe lado a lado;
4. normalização de propriedades comparáveis;
5. diff;
6. preflight;
7. PUT em múltiplos alvos;
8. GET de verificação após PUT.

NÃO copiar campos secretos/opacos sem revisão.

GATE 9:

- `/api/internal` aparece PRESENT em PROD-01/PROD-02 e MISSING em PROD-03;
- mudança segura em PROD-01 + PROD-02 executa preflight;
- cada target recebe resultado próprio;
- backend relê estado e verifica pós-condição.

**REFERÊNCIAS**

- Web App detail + PUT + DELETE, schema `Application`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L14627-L14784
- Web App list:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L15039-L15095

---

# 10. PROTOCOLO DE MUTAÇÃO MULTI-INSTÂNCIA

APLICAR A TODA ESCRITA FLEET.

ORDEM OBRIGATÓRIA:

```text
1 SELECT TARGETS
2 PREFLIGHT
3 PREVIEW
4 CONFIRM
5 EXECUTE
6 VERIFY
7 REPORT
```

## 10.1 Preflight

RETORNAR por target:

```text
READY
RESOURCE_MISSING
DEPENDENCY_MISSING
FORBIDDEN
OFFLINE
UNSUPPORTED
```

## 10.2 Execute

ANTES DA ESCRITA:

- repetir validação crítica para reduzir estado stale;
- impor timeout;
- limitar concorrência;
- nunca tratar target B como continuação transacional de target A.

## 10.3 Verify

APÓS PUT/DELETE/CREATE:

- reler recurso quando houver estado consultável;
- comparar estado esperado;
- marcar `SUCCESS` somente após confirmação quando a operação exigir verificação.

## 10.4 Report

EXEMPLO:

```text
PROD-01   SUCCESS
PROD-02   SUCCESS
PROD-03   FAILED_FORBIDDEN
```

NÃO prometer atomicidade global.

**REFERÊNCIAS**

- A SysAdmin API define permissões específicas por endpoint; exemplo Web Apps exige `%Admin_Secure:U`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L14627-L15095
- Process operations exigem `%Admin_Operate:U`:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6265-L6383

---

# 11. FASE 3 — TASK MANAGEMENT

OBJETIVO: aplicar FleetExecutor + Resource Matrix a tarefas.

CONSUMIR:

```http
GET  /v2/tasks
GET  /v2/task
PUT  /v2/task
DELETE /v2/task
POST /v2/task/run
POST /v2/task/suspend
POST /v2/task/resume
GET  /v2/task/upcoming
```

IMPLEMENTAR:

1. matriz de tarefas;
2. detalhe/schedule comparison;
3. `run` em target explícito;
4. suspend/resume;
5. mutation multi-target somente após preflight;
6. resultado por target.

DEMO:

- `DemoCleanup` presente nos três;
- `DemoAudit` apenas PROD-02.

GATE 11:

- matriz correta;
- run funciona no target escolhido;
- tarefa exclusiva de PROD-02 não é simulada nos demais targets.

**REFERÊNCIAS**

- Task run/resume/suspend:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L13884-L14050
- Task list:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L14130-L14220

---

# 12. FASE 4 — PERMISSIONS E SECURITY

OBJETIVO: cobrir segurança sem transformar o MVP em clonador irrestrito de configuração sensível.

## 12.1 Users / Roles / Resources

CONSUMIR:

```http
GET /v2/security/users
GET /v2/security/roles
GET /v2/security/resources
GET /v2/security/sql-privileges
GET /v2/security/sql-admin-privileges
```

PRIMEIRO ENTREGAR leitura/matriz.

DIFERENCIAR:

```text
RESOURCE_MISSING
PRINCIPAL_MISSING
PERMISSION_MISSING
SAME
DIFFERENT
```

MUTAÇÃO somente após a matriz estar estável.

**REFERÊNCIAS**

- Resources:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L10865-L10960
- Roles:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L11136-L11240
- SQL admin privileges:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L11503-L11686
- SQL privileges:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L12013-L12120
- Users:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L12838-L12940

---

# 13. FASE 5 — WALLET, X509, OAUTH

OBJETIVO: entregar presença e metadados antes de escrita sensível.

## 13.1 Wallet

CONSUMIR:

```http
GET /v2/wallet/collections
GET /v2/wallet/secrets
PUT /v2/wallet/secret
DELETE /v2/wallet/secret
```

REGRAS:

- matriz mostra nome/tipo/presença;
- nunca enviar valor secreto para a matriz Angular;
- criação de segredo exige entrada explícita do usuário;
- Quarkus usa valor somente durante operação;
- descartar valor após chamada;
- não logar request body sensível.

**REFERÊNCIAS**

- Wallet collections:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L14382-L14446
- Wallet secret mutation:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L14447-L14552
- Wallet secret names/types:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L14553-L14626

## 13.2 X509

CONSUMIR:

```http
GET /v2/security/x509-credentials
GET /v2/security/x509-credential
GET /v2/security/x509-credential/certificate
```

MVP:

- presence matrix;
- metadata;
- subject/serial/expiration quando disponíveis;
- escrita somente se contrato e segurança estiverem validados.

**REFERÊNCIA**

- X509 credential/detail/list:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L13028-L13380

## 13.3 OAuth2

MVP:

- listar client configurations;
- server definitions;
- resource servers;
- comparar configuração não secreta;
- não clonar secret automaticamente.

**REFERÊNCIA**

- OAuth2 APIs iniciam em client configuration e cobrem server definitions/resource servers:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L8594-L10450

GATE 13:

- Security/Secrets atende leitura fleet;
- nenhum segredo aparece no browser/log;
- 403 de uma instância aparece como estado daquela instância.

---

# 14. FASE 6 — SYSTEM / RUNTIME

OBJETIVO: comparar saúde das instâncias em uma tela.

CONSUMIR:

```http
GET /v2/monitor/dashboard/main
GET /v2/monitor/dashboard/system-resources
GET /v2/monitor/system-usage
GET /v2/monitor/system-usage/shared-memory
GET /v2/devices
```

UI:

```text
INSTANCE | LOAD/CPU | MEMORY | DISK | PROCESSES | STATUS
```

PRIORIZAR leitura.

NÃO implementar mutação de device no primeiro corte, salvo necessidade clara de demonstração.

GATE 14:

- cards/rows das três instâncias;
- valores não são misturados entre targets;
- target offline aparece como offline.

**REFERÊNCIAS**

- Monitor dashboard main/system resources:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L4944-L5025
- System usage/shared memory:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L5067-L5148
- Devices:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L1541-L2064

---

# 15. FASE 7 — UNIFIED EVENTS

OBJETIVO: agregar fontes documentadas. NÃO prometer “todos os logs do IRIS”.

PROVIDERS:

```text
SecurityAuditEventProvider
TaskEventProvider
MonitorAlertEventProvider
```

CONTRATO COMUM:

```text
instanceId
instanceName
source
timestamp
severity/type
user
message/details
```

PRIMEIRO PROVIDER:

- Security Audit.

IMPORTANTE:

`/v2/security/audit/records` é **POST** na especificação atual.

NÃO:

- montar filesystem do IRIS para raspar logs;
- ler `messages.log` como fonte de produto;
- chamar journal de “application log”.

GATE 15:

- eventos exibem `INSTANCE`;
- uma fonte indisponível não derruba as demais;
- filtros de tempo funcionam sem misturar timezone/instância.

**REFERÊNCIAS**

- Audit records (`POST /v2/security/audit/records`):  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6991-L7100
- Monitor dashboard para alertas/contadores:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L4944-L5025

---

# 16. FASE 8 — FLEET QUERY

OBJETIVO: implementar a Community Opportunity sem contaminar o núcleo administrativo.

MÓDULO ISOLADO:

```text
query/
  FleetQueryService
  IrisJdbcConnectionFactory
  SelectOnlyGuard
  QueryResultLimiter
```

USAR:

- InterSystems JDBC Type 4;
- connection URL `jdbc:IRIS://host:1972/NAMESPACE`;
- pool/conexão por target;
- timeout por target;
- limite de linhas;
- resultados separados por instância.

REGRAS DE SEGURANÇA:

1. aceitar uma única statement;
2. permitir somente SELECT no MVP;
3. rejeitar DDL/DML;
4. rejeitar multi-statement;
5. aplicar timeout JDBC;
6. aplicar max rows;
7. preferir credencial/role com privilégio somente leitura para demo;
8. nunca executar o mesmo SQL por concatenação de parâmetros internos.

NÃO usar ODBC.

NÃO substituir SysAdmin REST por JDBC.

GATE 16:

```sql
SELECT COUNT(*) FROM Demo.Person
```

executa em pelo menos PROD-01 + PROD-02 e retorna:

```text
INSTANCE | STATUS | RESULT | ELAPSED
```

**REFERÊNCIAS**

- JDBC Type 4/JDBC 4.2:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=BTPI_jdbc
- URL JDBC:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=AFL_jdbc
- Community Edition expõe SuperServer 1972:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ACLOUD

---

# 17. FRONTEND — ORDEM DE IMPLEMENTAÇÃO

EXECUTAR NESTA ORDEM:

```text
1 shell/layout
2 instances
3 processes
4 resource-matrix genérica
5 web-apps
6 tasks
7 permissions/security
8 secrets
9 system
10 logs/events
11 fleet-query
12 operation preview/result
```

COMPONENTES REUTILIZÁVEIS:

```text
shared/
  instance-selector/
  resource-matrix/
  configuration-diff/
  operation-preview/
  confirmation-dialog/
  fleet-operation-result/
  instance-status-badge/
```

REGRAS:

- toda entidade fleet mostra instância;
- nunca esconder falha parcial;
- não executar mutation em toggle imediato;
- preview e confirmação são obrigatórios;
- segredos jamais entram em tabela de comparação.

**REFERÊNCIAS**

Cada tela deve declarar no próprio README/feature quais endpoints SysAdmin utiliza. A matriz de cobertura é ancorada no contrato oficial:  
https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json

---

# 18. NGINX / FRONTEND CONTAINER

DOCKERFILE:

```text
STAGE 1: Node LTS
- npm ci
- npm run build

STAGE 2: nginx
- copiar dist
- copiar nginx.conf
- servir SPA
```

NGINX:

```text
/         -> Angular static
/api/*    -> http://backend:8080/api/*
```

NÃO proxyar `/api/admin` diretamente para IRIS.

GATE 18:

- `http://localhost:8080` abre UI;
- refresh de rota Angular não retorna 404;
- `/api/*` chega ao Quarkus;
- browser não conhece credenciais/endereço interno IRIS além de nomes exibidos como metadado.

**REFERÊNCIA IRIS**

A topologia preserva IRIS como servidor gerenciado e mantém seus serviços 52773/1972 acessados pelo backend, compatível com as portas documentadas para Community Edition:  
https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ACLOUD

---

# 19. BACKEND CONTAINER

USAR:

```text
Java 21
Quarkus JVM
```

BUILD MULTI-STAGE:

```text
STAGE 1 Maven/JDK
- copiar pom
- resolver deps
- copiar src
- mvn verify/package

STAGE 2 JRE
- copiar quarkus-app
- executar quarkus-run.jar
```

DEPENDÊNCIAS MÍNIMAS:

```text
Quarkus REST
Quarkus REST Client
Mutiny
Jackson
Bean Validation
SmallRye OpenAPI
Health
InterSystems JDBC Driver  # Fleet Query only
```

PROIBIDO depender de JAR instalado manualmente na máquina do desenvolvedor.

O driver deve entrar no build reproduzível.

**REFERÊNCIAS**

- InterSystems Java connectivity é baseada no JDBC driver Type 4:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=BJAVA_INTRO
- JDBC driver support:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=BTPI_jdbc

---

# 20. TESTES

## 20.1 Backend unitário

COBRIR:

```text
InstanceRegistry
InstanceEndpointPolicy
FleetExecutor
PreflightService
Process normalization
Resource Matrix normalization
Secret redaction
SelectOnlyGuard
```

## 20.2 Backend integração

COBRIR contra IRIS real em Docker:

```text
/info
/v2/processes
/v2/process
/v2/web-apps
/v2/tasks
security lists
monitor
Fleet Query JDBC
```

## 20.3 Frontend

COBRIR:

```text
instance attribution
matrix PRESENT/MISSING/DIFFERENT/OFFLINE
partial failure rendering
confirmation before destructive mutation
secret fields not rendered
```

## 20.4 Falha obrigatória

DURANTE teste:

1. parar `iris-prod2`;
2. repetir Process Explorer;
3. PROD-01 e PROD-03 devem continuar visíveis;
4. PROD-02 deve retornar OFFLINE;
5. página não pode falhar globalmente.

**REFERÊNCIAS**

- A API documenta 401/403/404/500 por operação; exemplo Process:  
  https://github.com/intersystems-community/sysadmin-api-specification/blob/master/mainspec_v2.json#L6169-L6441
- Containers são suporte oficial para ambientes repetíveis de IRIS:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ADOCK

---

# 21. PIPELINE DE BUILD LOCAL

EXECUTAR SEM EXCEÇÃO AO FECHAR MARCO:

```bash
docker compose down --remove-orphans
docker compose config
docker compose build --pull
docker compose up -d
docker compose ps
docker compose logs --no-color
```

PARA RESET LIMPO DO DEMO:

```bash
docker compose down -v --remove-orphans
docker compose build --no-cache
docker compose up -d
```

SMOKE TEST MÍNIMO:

```text
1 frontend HTTP 200
2 backend health UP
3 backend -> PROD-01 /info
4 backend -> PROD-02 /info
5 backend -> PROD-03 /info
6 fleet /processes retorna 3 targets
7 Web Apps matrix responde
8 Fleet Query executa SELECT em >= 2 targets
```

NÃO declarar pronto se apenas `docker compose build` passou.

**REFERÊNCIAS**

- Docker Compose é documentado pela InterSystems para execução multi-container:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ADOCK
- Community Edition em container:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=ACLOUD

---

# 22. CI

MÍNIMO:

```text
job backend-test
job frontend-test
job docker-build
```

ANTES DO RELEASE:

```text
job compose-smoke
```

REGRAS:

- fixar versão do IRIS usada no release;
- fixar versão Java/Node;
- registrar commit/revisão da SysAdmin API usada como contrato;
- não depender de cache Maven/npm local;
- não usar secret real em workflow público.

OPCIONAL APÓS MVP:

- publicar imagens no GHCR;
- online demo;
- SBOM/scanner.

**REFERÊNCIAS**

- InterSystems destaca containers como adequados a CI/CD e DevOps:  
  https://docs.intersystems.com/irislatest/csp/docbook/DocBook.UI.Page.cls?KEY=PAGE_deployment_containers
- Template oficial já contém workflows que podem ser avaliados e adaptados, não copiados cegamente:  
  https://github.com/intersystems-community/intersystems-iris-dev-template

---

# 23. SEQUÊNCIA DE ENTREGA

## MARCO A — INFRA

ENTREGAR:

```text
Docker Compose
3 IRIS
backend vazio saudável
frontend vazio saudável
network
ports
volumes
```

SAÍDA: ambiente sobe em um comando.

## MARCO B — PRIMEIRA PROVA FLEET

ENTREGAR:

```text
InstanceRegistry
Auth baseline
/info
FleetExecutor
/v2/processes
Angular Process table
```

SAÍDA: buscar processo sem saber instância.

## MARCO C — FLAGSHIP COMPLETO

ENTREGAR:

```text
process detail
suspend
resume
terminate
partial failure
confirmation
```

SAÍDA: ação roteada por `(instance, PID)`.

## MARCO D — FRAMEWORK DE RECURSO

ENTREGAR:

```text
Resource Presence Matrix
configuration diff
preflight
execute
verify
Web Apps
```

SAÍDA: `/api/internal` revela drift entre PROD-01/PROD-02/PROD-03.

## MARCO E — COBERTURA DO CONCURSO

ENTREGAR:

```text
Tasks
Users/Roles/Resources
Wallet
X509
OAuth
System
Events
```

SAÍDA: cada área exigida possui visão fleet funcional.

## MARCO F — COMMUNITY OPPORTUNITY

ENTREGAR:

```text
JDBC driver
Fleet Query
SELECT-only guard
row limit
timeout
per-instance result
```

SAÍDA: mesma query em múltiplos IRIS.

## MARCO G — RELEASE

ENTREGAR:

```text
seed demo
error UX
README English
architecture diagram
install steps
video/demo script
clean build
online demo se disponível
```

**REFERÊNCIAS**

- O concurso exige as áreas administrativas, Community Edition, Open Source e README em inglês:  
  https://community.intersystems.com/post/intersystems-programming-contest-build-your-own-management-portal
- Technology Bonuses incluem Docker e Community Opportunity; não justificam introduzir tecnologia inútil ao produto:  
  https://community.intersystems.com/post/technology-bonuses-intersystems-programming-contest-build-your-own-management-portal

---

# 24. DEFINITION OF DONE DO MVP

CONSIDERAR PRONTO SOMENTE QUANDO:

- [ ] template limpo;
- [ ] nenhum Python/WSGI/ODBC sem requisito;
- [ ] cinco serviços Compose iniciam;
- [ ] três IRIS são independentes;
- [ ] `/info` valida os três;
- [ ] Process Explorer agrega os três;
- [ ] `(instanceId,pid)` é a identidade do processo;
- [ ] uma instância offline não derruba chamada fleet;
- [ ] process detail funciona;
- [ ] pelo menos uma ação de processo funciona no target correto;
- [ ] Web Apps possui presence matrix;
- [ ] Web App multi-edit executa preflight + confirmation + verify;
- [ ] Tasks possui matrix e action;
- [ ] Users/Roles/Resources possui visão fleet;
- [ ] Wallet/X509/OAuth possui visão de presença;
- [ ] secret value não é exposto em Angular/log;
- [ ] System mostra cada target separadamente;
- [ ] Events mostra `instance` em toda linha;
- [ ] Fleet Query usa JDBC e SELECT-only;
- [ ] Fleet Query roda em pelo menos duas instâncias;
- [ ] frontend não acessa IRIS diretamente;
- [ ] credentials não são persistidas no browser;
- [ ] `docker compose build` passa;
- [ ] `docker compose up -d` passa;
- [ ] smoke tests reais passam;
- [ ] logs foram inspecionados;
- [ ] README em inglês contém instalação e arquitetura;
- [ ] demo reproduz exatamente o roteiro do concurso.

---

# 25. REGRA FINAL DE ENGENHARIA

ANTES DE ADICIONAR QUALQUER TECNOLOGIA, RESPONDER:

```text
QUAL RESPONSABILIDADE EXCLUSIVA ESTA TECNOLOGIA POSSUI NESTA SPEC?
```

SE A RESPOSTA FOR VAZIA:

```text
NÃO ADICIONAR.
```

PARA ESTE MVP:

```text
Angular        -> apresentação fleet
Quarkus        -> boundary externo, segurança, orchestration fleet
SysAdmin REST  -> administração IRIS
JDBC           -> Fleet Query somente
ObjectScript   -> fixtures controladas de demonstração, se necessárias
Docker Compose -> ambiente reproduzível com três IRIS
```

AUSENTES POR DECISÃO:

```text
Python
Embedded Python
Flask
WSGI
ODBC
ECP
RAG/LLM
```

ESSA AUSÊNCIA É PARTE DA ARQUITETURA.
