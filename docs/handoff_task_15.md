# Handoff: Contexto e Guia para Execução da TASK-15

> **Destino:** Próximo Agente / Desenvolvedor  
> **Foco:** `[TASK-15] Boletins de Medição de Obras e Relatório RAE da Caixa (Fase 4)`  
> **Data de Geração:** 2026-10-28  
> **Dependência concluída:** `[TASK-14] Relação 1:N de Licitações, Homologação VRPL e Emissão de AIO`

---

## 1. O que a TASK-14 deixou pronto

A Fase 3 está no `core-service` e no cockpit:

- Agregado `Licitacao` em `br.com.govflow.core.domain.model.licitacao` (relação 1:N com `Convenio`).
- REST em `/api/v1/convenios/{convenioId}/licitacoes/**` (cadastro, homologação, VRPL, AIO).
- Modal da Fase 3 no cockpit (`licitacoes-fase3-modal.component.ts`).
- Flyway `V23` com `parecer_tecnico_vrpl` e `observacoes_diligencia_vrpl`.

### TravaAntigrosa (obrigatória na TASK-15)

Antes de persistir qualquer Boletim de Medição, chamar:

```java
validarAioParaMedicaoUseCase.garantirAioEmitida(licitacaoId);
```

Porta: [`ValidarAioParaMedicaoUseCase`](file:///c:/projetos/consultoria/consultoria/services/core-service/src/main/java/br/com/govflow/core/application/port/in/ValidarAioParaMedicaoUseCase.java)

Implementação: [`LicitacaoService.garantirAioEmitida`](file:///c:/projetos/consultoria/consultoria/services/core-service/src/main/java/br/com/govflow/core/application/service/LicitacaoService.java)

Exceção: `AioNaoEmitidaException` (HTTP 422, errorCode `AIO_NAO_EMITIDA`).

Endpoint de exercício: `POST /api/v1/convenios/{convenioId}/licitacoes/{licitacaoId}/validar-aio-medicao` (204 se AIO `EMITIDO`).

A Medição deve referenciar a `Licitacao` (não só o Convênio): um convênio pode ter certames com AIO e outros ainda bloqueados.

---

## 2. Escopo da TASK-15 (backlog)

- Cálculo automático de percentuais físicos acumulados e saldo contratual remanescente.
- Associação obrigatória da Nota Fiscal ao boletim de medição.
- Conciliação do RAE da Caixa com valores glosados ou liberados.

Documentos de referência:

- [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/consultoria/consultoria/docs/clickup_tasks_backlog.md) — TASK-15
- [`docs/architecture/15_deep_research_fase_4_execucao_fisica_medicoes_e_rae.md`](file:///c:/projetos/consultoria/consultoria/docs/architecture/15_deep_research_fase_4_execucao_fisica_medicoes_e_rae.md)
- Flyway `V9` (tabela de medições, se já existir)

---

## 3. Guardrails

- Hexagonal: domínio puro, REST só via inbound ports.
- Multi-tenant `X-Tenant-Id` + escopo de `AGENTE` em `tb_usuario_prefeituras`.
- Sem fallback HTTP silencioso no frontend.
- Atualizar `CONTEXT.md` (Medicao, RAE) e o doc 23 ao implementar.
