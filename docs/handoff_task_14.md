# Handoff: Contexto e Guia para Execução da TASK-14

> **Destino:** Próximo Agente Antigravity / Desenvolvedor  
> **Foco:** `[TASK-14] Relação 1:N de Licitações, Homologação VRPL e Emissão de AIO (Fase 3)`  
> **Data de Geração:** 2026-09-26  
> **Status do Repositório:** 100% dos testes passando no monorepo (**298/298 testes automatizados verdes**, zero falhas). Todos os containers Docker sincronizados e saudáveis.

---

## 1. Estado Atual do Ecossistema GovFlow (Após Conclusão da TASK-13)

O ecossistema GovFlow concluiu integralmente a **TASK-13 — Gestão de Cláusula Suspensiva: Três Pilares da Caixa (Fase 2)** com sincronização ponta a ponta:

- **`core-service` (:8081)**:
  - **Domínio DDD**: Agregados [Convenio.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/domain/model/convenio/Convenio.java) e [CondicionanteSuspensiva.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/domain/model/convenio/CondicionanteSuspensiva.java) com validação de BDI, prazos de 180 dias corridos, controle de diligências da Caixa e regra de trava para superação apenas com 100% dos 3 pilares aprovados (`ENGENHARIA_PROJETOS_SINAPI`, `LICENCIAMENTO_AMBIENTAL` e `TITULARIDADE_IMOVEL`).
  - **Armazenamento S3/MinIO**: [MinioClausulaSuspensivaStorageAdapter.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/out/storage/MinioClausulaSuspensivaStorageAdapter.java) com bucket `govflow-documentos` e criação resiliente.
  - **Mensageria EDA**: [RabbitMQClausulaSuspensivaEventPublisherAdapter.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/out/messaging/RabbitMQClausulaSuspensivaEventPublisherAdapter.java) emitindo `core.clausula-suspensiva.superada` e `core.clausula-suspensiva.alerta-prazo`.
  - **REST API & Multi-Tenancy**: [ClausulaSuspensivaController.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/ClausulaSuspensivaController.java) e isolamento multi-tenant validado por teste de integração.
  - **Suíte de Testes**: **147 testes unitários e de integração** no `core-service` (100% verde).
- **`api-gateway` (:8080)**:
  - Rota `core-convenios` mapeada em [GatewayRoutesConfig.java](file:///c:/projetos/estudo%20spring/govflow/gateway/src/main/java/br/com/govflow/gateway/infrastructure/config/GatewayRoutesConfig.java) direcionando `/api/v1/convenios/**` ao `core-service` com preservação de headers de tenant.
  - **Suíte de Testes**: **21 testes reativos** (100% verde).
- **`frontend` (Angular 19+ Standalone)**:
  - Modal interativo [clausula-suspensiva-modal.component.ts](file:///c:/projetos/estudo%20spring/govflow/frontend/src/app/features/convenios/components/clausula-suspensiva-modal/clausula-suspensiva-modal.component.ts) integrado ao [convenio-cockpit-page.component.ts](file:///c:/projetos/estudo%20spring/govflow/frontend/src/app/features/convenios/pages/convenio-cockpit/convenio-cockpit-page.component.ts).
  - Cronômetro de prazo fatal, semáforo de criticidade, 3 cards com ações de aprovação técnica, registro de diligências, prorrogação excepcional e upload de Termo de Retirada.
  - [clausula-suspensiva.service.ts](file:///c:/projetos/estudo%20spring/govflow/frontend/src/app/features/convenios/services/clausula-suspensiva.service.ts) com fallback mock resiliente.
  - **Suíte de Testes**: **66 testes unitários** Karma/ChromeHeadless passando (100% verde).
- **Outros Serviços**:
  - `transferegov-service`: **48 testes** (100% verde).
  - `whatsapp-service`: **16 testes** (100% verde).
  - **Total Geral**: **298 testes automatizados** passando com zero falhas no monorepo.

---

## 2. Escopo e Especificação da TASK-14

### 2.1 Identificação
* **Tarefa:** `[TASK-14] Relação 1:N de Licitações, Homologação VRPL e Emissão de AIO (Fase 3)`
* **Localização no Backlog:** [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L348-L362)
* **Documento Técnico de Referência:** [`docs/architecture/14_deep_research_fase_3_licitacoes_vrpl_e_aio.md`](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/14_deep_research_fase_3_licitacoes_vrpl_e_aio.md)
* **Serviços Afetados:** `core-service` (Backend DDD/Hexagonal), `api-gateway` (Rotas REST), MinIO (Dossiê do VRPL) e `frontend` (Cockpit do Convênio / Tab Fase 3).
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-03]`, `[TASK-13]`, Flyway `V4` e `V8` (já existentes no banco).
* **Fundamentação Legal:** Nova Lei de Licitações e Contratos (Lei nº 14.133/2021), Decreto Federal nº 11.531/2023, Portaria Conjunta MGI/MF/CGU nº 33/2023 e Manual Normativo Caixa MN AE099.

---

### 2.2 O Que a Task Entrega

1. **Relação 1:N entre Convênio e Licitações**:
   - Um único convênio federal comporta múltiplos certames licitatórios independentes (ex: Lote 1 — Obras Civis, Lote 2 — Equipamentos Hospitalares, Lote 3 — Fiscalização Externa).
   - Cada licitação possui número do processo administrativo, número da licitação (`Concorrência 002/2024`), modalidade (Lei 14.133), critério de julgamento, regime de execução, valor estimado (vinculado à SPA da Fase 2) e valor homologado.
   - Cálculo automático da **economia de licitação** (percentual de desconto e valor absoluto).

2. **Publicidade e Eficácia Jurídica (PNCP & DOU)**:
   - Registro de links oficiais (`link_pncp`, `link_transferegov`, `link_sistema_compras`).
   - Datas do ciclo licitatório: publicação do edital, abertura de propostas e homologação pelo Prefeito.

3. **Verificação do Resultado do Processo Licitatório (VRPL)**:
   - Submissão formal do VRPL no Transferegov com número de protocolo (`numero_vrpl_transferegov`) e data de envio.
   - Estados de VRPL: `NAO_ENVIADO`, `EM_ANALISE_CAIXA`, `DILIGENCIA`, `ACEITO_HOMOLOGADO`, `REJEITADO`.
   - Registro de parecer técnico de VRPL emitido pela Caixa GIGOV com data de aceite (`data_aceite_vrpl`).

4. **Autorização de Início de Objeto (AIO) e a Regra Antigrosa**:
   - Controle do número da AIO expedida pela Caixa (`numero_aio`) e data de emissão (`data_emissao_aio`).
   - Status da AIO: `NAO_EMITIDO`, `SOLICITADO`, `EMITIDO`.
   - **Regra de Ouro da Engenharia Federal (Trava Antigrosa)**: O sistema deve validar e impedir a emissão de Ordem de Serviço ou criação de Boletins de Medição de Obras (Fase 4) para licitações que não possuam AIO deferida (`status_aio = 'EMITIDO'`).

5. **Custódia Segura do Dossiê VRPL no MinIO (`govflow-documentos`)**:
   - `s3_key_edital`: Edital e anexos.
   - `s3_key_termo_homologacao`: Termo de homologação municipal.
   - `s3_key_proposta_vencedora`: Proposta final com planilha contratada e BDI da contratada.
   - `s3_key_ata_sessao`: Ata da sessão pública com histórico de lances.
   - `s3_key_parecer_vrpl`: Parecer técnico conclusivo de VRPL da Caixa.
   - `s3_key_autorizacao_aio`: Cópia do documento oficial da AIO.

6. **Eventos de Domínio via RabbitMQ**:
   - `core.licitacao.vrpl-aceito`: Notifica a aprovação do VRPL pela Caixa.
   - `core.licitacao.aio-emitida`: Notifica que a licitação está apta para início de obra e medições.

---

## 3. Estrutura do Banco de Dados (Flyway V4 e V8 Já Aplicadas)

A tabela `core_schema.tb_licitacoes` já se encontra estruturada nas migrations [V4__create_licitacoes_table.sql](file:///c:/projetos/estudo%20spring/govflow/flyway/sql/V4__create_licitacoes_table.sql) e [V8__enhance_fase_3_licitacoes_vrpl_fields.sql](file:///c:/projetos/estudo%20spring/govflow/flyway/sql/V8__enhance_fase_3_licitacoes_vrpl_fields.sql):

```sql
-- Principais colunas disponíveis em core_schema.tb_licitacoes:
id UUID PRIMARY KEY,
tenant_id UUID NOT NULL,
convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id),
prefeitura_id UUID NOT NULL REFERENCES core_schema.tb_prefeituras(id),
numero_processo_administrativo VARCHAR(50),
numero_licitacao VARCHAR(50) NOT NULL,
modalidade VARCHAR(50) NOT NULL,
criterio_julgamento VARCHAR(50),
regime_execucao VARCHAR(50),
objeto TEXT NOT NULL,
valor_estimado NUMERIC(15, 2) NOT NULL,
valor_homologado NUMERIC(15, 2),
percentual_desconto NUMERIC(5, 2),
situacao VARCHAR(50) NOT NULL DEFAULT 'PLANEJAMENTO',
data_publicacao_edital DATE,
data_abertura_propostas DATE,
data_homologacao DATE,
link_pncp VARCHAR(500),
link_transferegov VARCHAR(500),
link_sistema_compras VARCHAR(500),
numero_vrpl_transferegov VARCHAR(50),
status_vrpl VARCHAR(30) NOT NULL DEFAULT 'NAO_ENVIADO',
data_envio_vrpl DATE,
data_aceite_vrpl DATE,
numero_aio VARCHAR(50),
data_emissao_aio DATE,
status_aio VARCHAR(30) NOT NULL DEFAULT 'NAO_EMITIDO',
cnpj_vencedor VARCHAR(18),
razao_social_vencedor VARCHAR(200),
s3_key_edital VARCHAR(500),
s3_key_termo_homologacao VARCHAR(500),
s3_key_proposta_vencedora VARCHAR(500),
s3_key_ata_sessao VARCHAR(500),
s3_key_parecer_vrpl VARCHAR(500),
s3_key_autorizacao_aio VARCHAR(500),
created_at TIMESTAMP WITH TIME ZONE,
updated_at TIMESTAMP WITH TIME ZONE
```

---

## 4. Plano de Implementação Sugerido

### 4.1 Backend (`core-service`)
1. **Domínio (`br.com.govflow.core.domain.model.licitacao`)**:
   - `Licitacao.java` (Entidade de Domínio com métodos de negócio: `homologar()`, `submeterVrpl()`, `aceitarVrpl()`, `emitirAio()`, cálculo de desconto).
   - Enums: `ModalidadeLicitacao`, `CriterioJulgamento`, `RegimeExecucao`, `SituacaoLicitacao`, `StatusVrpl`, `StatusAio`.
   - Exceções de Domínio: `LicitacaoNaoEncontradaException`, `RegraNegocioLicitacaoException`, `AioNaoEmitidaException`.
   - Eventos de Domínio: `VrplAceitoEvent`, `AioEmitidaEvent`.
   - Testes unitários puros de domínio (`LicitacaoTest.java`).
2. **Portas e Casos de Uso (`br.com.govflow.core.application.port`)**:
   - `CadastrarLicitacaoUseCase`, `HomologarLicitacaoUseCase`, `SubmeterVrplUseCase`, `AceitarVrplUseCase`, `EmitirAioUseCase`, `ConsultarLicitacoesConvenioUseCase`.
   - DTOs de entrada e saída (`LicitacaoDto`, `CadastrarLicitacaoCommand`, `HomologarLicitacaoCommand`, `SubmeterVrplCommand`, `EmitirAioCommand`).
   - Porta de saída: `LicitacaoRepositoryPort`.
   - Serviço de Aplicação: `LicitacaoService.java` com testes unitários Mockito (`LicitacaoServiceTest.java`).
3. **Persistência & Adapters (`br.com.govflow.core.infrastructure`)**:
   - `LicitacaoJpaEntity.java` mapeando `core_schema.tb_licitacoes` estendendo `BaseTenantEntity` com `@TenantId`.
   - `SpringDataLicitacaoRepository.java` e `LicitacaoRepositoryAdapter.java`.
   - Teste de integração de repositório e isolamento multi-tenant (`MultiTenantLicitacaoIsolationIntegrationTest.java`).
4. **REST Controller & Gateway**:
   - `LicitacaoController.java` (`/api/v1/convenios/{convenioId}/licitacoes/**`).
   - `LicitacaoControllerTest.java` com MockMvc.
   - Atualização do gateway se necessário (já possui rota para `/api/v1/convenios/**`).

### 4.2 Frontend (`frontend` Angular 19+)
1. **Model & Service**:
   - `licitacao.model.ts` com tipagem alinhada aos enums e DTOs.
   - `licitacao.service.ts` consumindo os endpoints via `HttpClient` com fallback resiliente.
2. **Componente de Licitações (Fase 3)**:
   - Componente ou Modal da Fase 3 (`licitacoes-fase3-modal.component.ts` ou tab no cockpit).
   - Visualização da relação 1:N (cards/tabela de licitações vinculadas).
   - Indicador de status de VRPL e badge de AIO com semáforo visual.
   - Formulário para submissão de VRPL e registro de emissão de AIO.
3. **Testes Unitários**:
   - Testes unitários para service e componente garantindo cobertura e regressão zero.

---

## 5. Cuidados Técnicos e Diretrizes Mandatórias

1. **Protocolo `/transparent-pairing`**:
   - Sempre forneça explicação concisa antes de executar comandos shell (`run_command`).
2. **Links Clicáveis Markdown**:
   - Use sempre links no formato `file:///path/to/file` com barras normais `/`.
3. **Isolamento de Tenant**:
   - Garantir que todas as consultas filtrem por `tenantId` e que os testes de integração usem CPF e CNPJ matematicamente válidos.
4. **Fechamento de Task (5 Etapas Obrigatórias)**:
   - Etapa 1 & 2: Conformidade e 0 regressões monorepo.
   - Etapa 3: Build de containers Docker (`docker compose up -d --build`).
   - Etapa 4: Atualizar backlog e criar handoff da próxima task (`TASK-15`).
   - Etapa 5: Git commit convencional padronizado e push.
