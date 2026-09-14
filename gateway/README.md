# API Gateway GovFlow — Entrega na branch `delta`

> **Para o administrador do repositório:** este README descreve o que foi implementado nesta branch, como validar e o que **não** entra no escopo. Use-o para decidir se faz merge em `main`.

## Resumo executivo

Foi implementado o **ponto de entrada unificado** do GovFlow (`api-gateway`) na porta **8080**, com Spring Cloud Gateway (Java 21), Clean Architecture enxuta, validação JWT in-memory, erros **RFC 7807**, rotas públicas/protegidas e propagação dos headers `X-Tenant-Id`, `X-User-Id` e `X-Correlation-Id` para os destinos internos.

Como os microsserviços reais ainda não existem, o `docker-compose.yml` inclui **stubs HTTP** (nginx) que ecoam os headers recebidos, permitindo provar roteamento e propagação sem o core/whatsapp/ai/transferegov.

Auditoria das etapas: [`docs/audits/test_results.json`](../docs/audits/test_results.json) — **5/5 done**.

## O que foi feito

| Item | Detalhe |
|------|---------|
| Módulo Maven | Pasta [`gateway/`](./) — pacote `br.com.govflow.gateway` |
| Arquitetura | `domain` → `application` (ports/use cases) → `infrastructure` (filtros, JWT, rotas) |
| Porta | `8080` (`GATEWAY_PORT`) |
| JWT | HMAC (`JWT_SECRET`); claims `tenant_id` + `sub`/`user_id`; sem consulta a banco |
| Rotas públicas (sem token) | `/api/v1/auth/**`, `/api/v1/whatsapp/webhook/**` |
| Rotas protegidas | `/api/v1/core/**`, `/api/v1/whatsapp/**`, `/api/v1/ai/**`, `/api/v1/transferegov/**` |
| Erro sem token | HTTP **401** + `application/problem+json` (RFC 7807) |
| Headers | Remove headers de segurança enviados pelo cliente e reinjeta valores do JWT + correlação |
| Compose | Serviços `api-gateway`, `stub-core`, `stub-transferegov`, `stub-whatsapp`, `stub-ai` |
| Env | Variáveis documentadas em [`.env.example`](../.env.example) |
| Testes | Unitários (`RoutePolicy`, `AuthenticateRequestService`) + script [`scripts/acceptance_check.py`](./scripts/acceptance_check.py) |

## Como foi feito (decisões técnicas)

1. **Clean Architecture enxuta** no gateway (política de rota + auth), alinhada a `docs/architecture/01_arquitetura_api_gateway_clean_arch.md` e aos pitfalls de Netty (sem JDBC/JPA no gateway).
2. **Rotas por prefixo de serviço** (`/api/v1/core/**` etc.), conforme C4 / critério de aceite — não pelas rotas granulares (`/prefeituras`, `/convenios`) que ficam para quando o core existir.
3. **Stubs nginx** na rede Docker (porta interna 80) no lugar dos microsserviços, só para esta etapa.
4. **Build via Docker** (`gateway/Dockerfile` com Maven + Temurin 21), adequado quando não há JDK 21 local.
5. **Fora desta entrega:** rate limiting Redis, circuit breaker, emissão de JWT pelo core, microsserviços reais.

## Como validar (checklist rápido)

Pré-requisito: Docker Desktop.

```powershell
cd consultoria
docker compose up -d stub-core stub-transferegov stub-whatsapp stub-ai api-gateway
```

Aceite automatizado (a partir da pasta `consultoria`):

```powershell
docker run --rm --add-host=host.docker.internal:host-gateway `
  -v "${PWD}/gateway/scripts/acceptance_check.py:/check.py:ro" `
  python:3.12-alpine sh -c "pip install -q PyJWT && python /check.py"
```

Esperado: `ALL ACCEPTANCE CHECKS PASSED`.

Smoke manual:

- Sem token em `/api/v1/core/...` → **401** JSON problem+json  
- Sem token em `/api/v1/auth/...` ou `/api/v1/whatsapp/webhook/...` → **não** 401 (proxy OK)  
- Com JWT válido → stub responde ecoando `x_tenant_id` e `x_correlation_id`

## Arquivos principais tocados

- `gateway/**` — código, Dockerfile, testes, script de aceite  
- `docker-compose.yml` — gateway + stubs  
- `docker/stubs/nginx-echo.conf` — eco de headers  
- `.env.example` — `JWT_SECRET`, URIs dos stubs  
- `docs/audits/test_results.json` — resultado do step-audit  
- Este `gateway/README.md` — nota de entrega para review/merge  

## Recomendação para merge

A entrega é **autocontida** e não altera schemas Flyway nem a Evolution API. É segura para merge em `main` do ponto de vista de infra existente, desde que o admin concorde com:

- stubs temporários no compose até os microsserviços existirem;  
- `JWT_SECRET` apenas local/dev (trocar em ambientes reais).
