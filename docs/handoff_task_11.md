# Handoff: Contexto e Guia para Execução da TASK-11

> **Destino:** Próximo Agente Antigravity / Desenvolvedor  
> **Foco:** `[TASK-11] Ingestão da API REST de Emendas Especiais (Emendas Pix)`  
> **Data de Geração:** 2026-09-24  
> **Status do Repositório:** 162/162 testes de backend passando (100% verde) + 31/31 testes de frontend Angular passando (100% verde).

---

## 1. Estado Atual do Ecossistema GovFlow (Após Conclusão da TASK-10)

O ecossistema GovFlow está totalmente integrado e validado com cobertura completa:
- **`transferegov-service` (:8082)**:
  - **TASK-09**: Pipeline de streaming ETL em memória (Zero Disk I/O) para dados diários do SICONV, Motor Data Quality de 6 dimensões, quarentena de anomalias em `tb_sincronizacao_anomalias` e persistência de convênios em `tb_sincronizacao_convenio`.
  - **TASK-10**: Motor de cálculo e monitoramento de criticidade de prazos (`DeadlineDetectorService`), avaliação de marcos temporais (`CLAUSULA_SUSPENSIVA`, `FIM_VIGENCIA`, `PRESTACAO_CONTAS`), semáforo de risco (`CRITICO`, `ATENCAO`, `REGULAR`), emissão de eventos RabbitMQ na exchange `govflow.events` com routing key `transferegov.prazo.alerta` e agendamento matinal (`DeadlineMonitoringScheduler`).
  - **Endpoints REST Consolidado**: `GET /api/v1/transferegov/radar-prazos` com KPIs de semáforo, agrupamento por município/prefeitura e lista de alertas detalhados com filtros dinâmicos; e `POST /api/v1/transferegov/radar-prazos/avaliar` para acionamento e disparo de alertas sob demanda.
- **`frontend` (Angular 19+ Standalone)**:
  - Cockpit completo de Radar de Prazos (`RadarPrazosPageComponent`) na rota `/radar-prazos`, integrado com o API Gateway (:8080).
  - Semáforo visual com 4 KPI cards interativos (Crítico, Atenção, Regular e Total Monitorado), barra de busca reativa por município/convênio/CNPJ, filtros rápidos por gravidade e alternância entre visão detalhada de convênios e visão consolidada por prefeitura.
  - Testes unitários com Karma/ChromeHeadless 100% verdes (31/31) e build de produção sem erros.
- **Suíte de Testes Geral**:
  - `transferegov-service`: 31 testes (0 falhas)
  - `core-service`: 94 testes (0 falhas)
  - `gateway`: 21 testes (0 falhas)
  - `whatsapp-service`: 16 testes (0 falhas)
  - **Total Backend**: 162 testes automatizados passando.
  - **Total Frontend**: 31 testes automatizados passando.

---

## 2. Escopo e Especificação da TASK-11

### 2.1 Identificação
* **Tarefa:** `[TASK-11] Ingestão da API REST de Emendas Especiais (Emendas Pix)`
* **Localização no Backlog:** [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L193-L208)
* **Serviços Afetados:** `transferegov-service` (Backend)
* **Prioridade:** Média (Amarelo)
* **Dependência (Blocked by):** `TASK-09` (Concluída ✅), `TASK-10` (Concluída ✅)
* **Regulação Federal / Compliance:** STF (ADPF 854) — Transparência ativa e conformidade na destinação de transferências especiais.

### 2.2 O Que a Task Entrega
1. Integração com a API aberta federal do Transferegov para Emendas Especiais (`/especiais`).
2. Consumo paginado resiliente com tratamento de paginação, rate-limiting e retries via Spring `RestClient`.
3. Mapeamento e persistência das transferências especiais, planos de trabalho e contas correntes vinculadas.
4. Identificação e flag de inconsistências ou ausência de relatórios de gestão nos prazos regulamentares.
5. Exposição de consultas e endpoints de auditoria de conformidade com a ADPF 854.

---

## 3. Sugestão de Passo a Passo para a TASK-11

1. **Planejamento (`plan-writing`)**:
   - Mapear os modelos e DTOs de retorno da API `/especiais` do Transferegov.
   - Definir a tabela de banco de dados (`tb_emendas_especiais`) ou evolução de schema no Flyway.
2. **Backend TDD (`tdd`)**:
   - Criar cliente REST com Spring `RestClient` em `br.com.govflow.transferegov.sync.client`.
   - Implementar pipeline de sincronização e detecção de pendências de relatórios de gestão (ADPF 854).
   - Scaffolding de testes de unidade e integração com mock de API REST federal.
3. **Validação & Regressão**:
   - Rodar suíte de testes de todos os serviços mantendo 100% verde.
