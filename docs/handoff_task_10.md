# Handoff: Contexto e Guia para Execução da TASK-10

> **Destino:** Próximo Agente Antigravity / Desenvolvedor  
> **Foco:** `[TASK-10] Radar Proativo de Prazos Críticos e Alertas de Vigência`  
> **Data de Geração:** 2026-09-24  
> **Status do Repositório:** Branch `main` (commit `b4ce7c1`), 150/150 testes automatizados passando (100% verde).

---

## 1. Estado Atual do Ecossistema GovFlow

O ecossistema está totalmente estabilizado, com build verde e zero débitos técnicos bloqueantes:
- **`transferegov-service` (:8082)**: Implementado na **TASK-09** com streaming ETL em memória (Zero Disk I/O) para dados diários do SICONV, Motor Data Quality de 6 dimensões, isolamento de anomalias em `tb_sincronizacao_anomalias` e persistência de convênios na tabela `transferegov_schema.tb_sincronizacao_convenio`.
- **Arquitetura Segregada (CQRS-Light)**:
  - `sync/` (Write Side): Ingestão, `SiconvStreamingPipeline`, `SiconvConvenioBatchWriter`, `MonitoredCnpjProvider`, agendador e `SincronizacaoController`.
  - `query/` (Read Side): Projeções de consulta, `ConvenioQueryController` e `ConvenioDTO`.
- **`core-service` (:8081)**: Refatorado com eliminação de *Feature Envy* e *Data Clumps*, adoção dos Value Objects `MandatoGestor` e `ArmazenamentoArquivo`, e isolamento multi-tenant estrito via `TenantInterceptor`.
- **Suíte de Testes**: 150/150 testes passando (`core-service`: 94, `transferegov-service`: 19, `gateway`: 21, `whatsapp-service`: 16).

---

## 2. Escopo e Especificação da TASK-10

### 2.1 Identificação
* **Tarefa:** `[TASK-10] Radar Proativo de Prazos Críticos e Alertas de Vigência`
* **Localização no Backlog:** [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L176-L190)
* **Serviços Afetados:** `transferegov-service` (Backend) e `frontend` (Angular 19+)
* **Prioridade:** Média (Amarelo)
* **Dependências:** `TASK-09` (Concluída ✅), `TASK-07` (Cockpit Frontend Base ✅)

### 2.2 O Que a Task Entrega
Motor de monitoramento diário de risco de transferências voluntárias federais que:
1. Avalia datas de vencimento de convênios:
   - `dataFimVigencia`: Término do prazo de vigência do instrumento.
   - `dataSuspensiva`: Limite para atendimento de cláusula suspensiva (Fase 2 da Caixa).
   - `dataLimitePrestacaoContas`: Limite para submissão da prestação final (Fase 9).
2. Calcula os dias restantes para expiração em relação à data corrente (`LocalDate.now()`).
3. Classifica os convênios em réguas de criticidade / semáforo de risco:
   - **Crítico (Vermelho)**: Menos de 15 dias restantes ou já vencido.
   - **Atenção (Amarelo)**: Entre 16 e 60 dias restantes.
   - **Regular (Verde)**: Mais de 60 dias restantes.
4. Disponibiliza o endpoint REST consolidado:
   - `GET /api/v1/transferegov/radar-prazos` com suporte a filtros por `uf`, `cnpj`, `nivelRisco` (`CRITICO`, `ATENCAO`, `REGULAR`) e agrupamento por município/prefeitura.
5. (Event-Driven / Mensageria): Emite eventos no RabbitMQ para a exchange `govflow.events` com routing-key `transferegov.prazo.alerta` quando detectada proximidade crítica (já configurada no `application.yml`).
6. Componente de Dashboard no Frontend Angular com semáforo visual e cards de convênios em risco.

---

## 3. Base Técnica Pronta no Código para Alavancar a TASK-10

- **Entidade Base**: [`SincronizacaoConvenioEntity.java`](file:///c:/projetos/estudo%20spring/govflow/services/transferegov-service/src/main/java/br/com/govflow/transferegov/persistence/entity/SincronizacaoConvenioEntity.java) já possui os campos:
  - `dataInicioVigencia`, `dataFimVigencia`, `dataLimitePrestacaoContas`, `dataSuspensiva`, `situacaoConvenio`, `instrumentoAtivo`.
- **Repositório Existente**: [`SincronizacaoConvenioRepository.java`](file:///c:/projetos/estudo%20spring/govflow/services/transferegov-service/src/main/java/br/com/govflow/transferegov/persistence/repository/SincronizacaoConvenioRepository.java) já indexa consultas por UF, CNPJ e situação.
- **Configuração de Mensageria**: [`RabbitMQTransfereConfig.java`](file:///c:/projetos/estudo%20spring/govflow/services/transferegov-service/src/main/java/br/com/govflow/transferegov/config/RabbitMQTransfereConfig.java) e [`application.yml`](file:///c:/projetos/estudo%20spring/govflow/services/transferegov-service/src/main/resources/application.yml#L40-L43) já definem a exchange `govflow.events` e a routing key `transferegov.prazo.alerta`.
- **Especificação Arquitetural**: [`docs/architecture/03_arquitetura_transferegov_service_cqrs.md`](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/03_arquitetura_transferegov_service_cqrs.md#L72) já prevê a inclusão de um `DeadlineDetectorService.java` e do endpoint de radar.

---

## 4. Sugestão de Passo a Passo para o Próximo Agente

1. **Planejamento (`plan-writing`)**:
   - Criar plano estruturado detalhando contratos DTO (`RadarPrazosDTO`, `AlertaConvenioDTO`, `NivelRisco`), queries otimizadas no repositório e estrutura do componente no Angular.
2. **Backend TDD (`tdd`)**:
   - Criar testes unitários para a régua de prazos (`DeadlineDetectorServiceTest`).
   - Implementar `DeadlineDetectorService` em `br.com.govflow.transferegov.sync.pipeline` ou `query.service`.
   - Adicionar queries de busca de convênios por janela temporal no repositório.
   - Criar `RadarPrazosController` em `br.com.govflow.transferegov.query.controller` atendendo a `GET /api/v1/transferegov/radar-prazos`.
3. **Frontend (`angular`)**:
   - Criar componente Angular de radar de prazos com semáforo visual (badges coloridas, filtros por nível de risco e tabela/cards responsivos).
4. **Validação & Regressão**:
   - Rodar testes em todos os microsserviços e certificar que a suíte permanece 100% verde.

---

## 5. Suggested Skills (Habilidades Recomendadas)

O próximo agente deve invocar as seguintes skills para a realização da TASK-10:
- **`plan-writing`**: Para elaboração do plano de implementação detalhado antes da escrita de código.
- **`tdd`**: Para scaffolding e testes de unidade do motor de cálculo de prazos e do endpoint REST.
- **`angular`**: Para scaffolding de componentes modernos do Angular (Signals, Standalone Components, semântica visual de semáforo).
- **`api-design-principles`**: Para garantir contratos REST intuitivos e idempotentes no endpoint `/radar-prazos`.

---

## 6. Referências a Artefatos do Repositório

- **Backlog Geral**: [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md)
- **Especificação CQRS**: [`docs/architecture/03_arquitetura_transferegov_service_cqrs.md`](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/03_arquitetura_transferegov_service_cqrs.md)
- **Handoff da Task Anterior**: [`docs/handoff_task_09.md`](file:///c:/projetos/estudo%20spring/govflow/docs/handoff_task_09.md)
- **Regras de Negócio e Vocabulário**: [`CONTEXT.md`](file:///c:/projetos/estudo%20spring/govflow/CONTEXT.md)
