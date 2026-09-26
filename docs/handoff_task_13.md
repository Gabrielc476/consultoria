# Handoff: Contexto e Guia para Execução da TASK-13

> **Destino:** Próximo Agente Antigravity / Desenvolvedor  
> **Foco:** `[TASK-13] Gestão de Cláusula Suspensiva: Três Pilares da Caixa (Fase 2)`  
> **Data de Geração:** 2026-09-25  
> **Status do Repositório:** 197/197 testes de backend passando (100% verde) + 53/53 testes de frontend Angular passando (100% verde). Total: **250/250 testes verdes**.

---

## 1. Estado Atual do Ecossistema GovFlow (Após Conclusão da TASK-12)

O ecossistema GovFlow concluiu a implementação integral do **Radar CAUC (Fase 0)** com sincronização ponta a ponta:

- **`core-service` (:8081)**:
  - **TASK-12**: Mapeamento JPA e domínio DDD para as 16 certidões fiscais e orçamentárias obrigatórias da IN STN nº 01/2021 e art. 25 da LRF em `tb_certidoes_cauc` (`core_schema`).
  - Motor de auto-seeding e avaliação de conformidade (`CaucService`) com transição automática de semáforos (`REGULAR`, `ALERTA`, `VENCIDA`) e bloqueio da prefeitura se houver pendência impeditiva.
  - Publicação de eventos de alerta via RabbitMQ na exchange `govflow.events` com routing key `core.cauc.alerta`.
  - Agendador matinal diário (`CaucDailyScheduler`) às 07:00 ativado por `@EnableScheduling`.
  - Endpoints REST `/api/v1/cauc/prefeituras/{id}`, `/api/v1/cauc/resumo`, `/api/v1/cauc/prefeituras/{id}/certidoes` e `/api/v1/cauc/avaliar`.
  - Isolamento multi-tenant robusto validado por teste de integração com schema tenancy.
- **`gateway` (:8080)**:
  - Rota `core-cauc` mapeada em `GatewayRoutesConfig` direcionando `/api/v1/cauc/**` com reescrita para o `core-service`.
- **`frontend` (Angular 19+ Standalone)**:
  - Integração do endpoint `/api/v1/cauc` no `RadarCaucService`.
  - Matriz de saúde fiscal reativa em `/radar-cauc` com métricas consolidadas e semáforos visuais por exigência.
  - Badge de status de regularidade fiscal do CAUC integrado no `HeaderComponent` e `MunicipioContextService`.
- **`transferegov-service` (:8082)** e **`whatsapp-service` (:8083)**:
  - Operacionais, saudáveis e integrados via mensageria RabbitMQ.
- **Métricas Globais de Qualidade**:
  - `core-service`: 112 testes (0 falhas)
  - `transferegov-service`: 48 testes (0 falhas)
  - `gateway`: 21 testes (0 falhas)
  - `whatsapp-service`: 16 testes (0 falhas)
  - `frontend`: 53 testes (0 falhas)
  - **Total:** 250 testes automatizados passando (100% verde).

---

## 2. Escopo e Especificação da TASK-13

### 2.1 Identificação
* **Tarefa:** `[TASK-13] Gestão de Cláusula Suspensiva: Três Pilares da Caixa (Fase 2)`
* **Localização no Backlog:** [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L332-L345)
* **Documento Técnico de Referência:** [`docs/architecture/13_deep_research_fase_2_gestao_clausula_suspensiva.md`](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/13_deep_research_fase_2_gestao_clausula_suspensiva.md)
* **Serviços Afetados:** `core-service` (Backend), `api-gateway` (Rotas), MinIO/S3 (Uploads de Laudos/Projetos) e `frontend` (Cockpit do Convênio / Modal Fase 2).
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-03]`, Flyway `V3` e `V7`.
* **Fundamentação Legal:** Decreto Federal nº 11.531/2023, Portaria Conjunta MGI/MF/CGU nº 33/2023, Manual Normativo da Caixa Econômica Federal MN AE099 (Engenharia Mandatária - GIGOV).

---

### 2.2 O Que a Task Entrega

1. **Os Três Pilares Condicionantes Obrigatórios da Caixa GIGOV**:
   - **Pilar 1 — Engenharia & Orçamento SINAPI (`ENGENHARIA_PROJETOS_SINAPI`)**:
     - Projetos básicos e executivos completos (arquitetura, estrutura, instalações).
     - Orçamento detalhado balizado por SINAPI/SICRO, memorial descritivo e Curva ABC de serviços e insumos.
     - BDI analítico dentro das faixas limites do TCU (Acórdão nº 2.622/2013 - Plenário).
     - ART/RRT quitada de elaboração e orçamento.
     - Emissão do Laudo de Análise de Engenharia (LAE) e Síntese do Projeto Aprovado (SPA).
   - **Pilar 2 — Licenciamento Ambiental (`LICENCIAMENTO_AMBIENTAL`)**:
     - Licença Prévia (LP) e Licença de Instalação (LI), ou Licença Ambiental Simplificada/Única.
     - Alternativa legal: Declaração de Inexigibilidade ou Dispensa emitida pelo órgão competente (ex: SUDEMA-PB ou órgão municipal com competência delegada).
   - **Pilar 3 — Comprovação de Titularidade Imobiliária (`TITULARIDADE_IMOVEL`)**:
     - Certidão de Inteiro Teor da Matrícula no Cartório de Registro de Imóveis (CRI) com data inferior a 30 dias em nome do Município.
     - Alternativas aceitas: Termo de Doação formalizado, Termo de Cessão de Uso por período superior à vida útil do bem público, ou Decreto de Desapropriação com Imissão Provisória na Posse transitada em julgado.

2. **Gestão de Prazos Fatais e Prorrogações**:
   - Controle do prazo fatal de **180 dias corridos** contados da publicação do convênio / contrato de repasse no Diário Oficial da União (DOU).
   - Rastreamento de prorrogação excepcional (`prorrogacao_solicitada`, `novo_prazo_prorrogado`) solicitada antes do vencimento do prazo original.
   - Cálculo dinâmico de dias restantes para o prazo fatal da cláusula suspensiva.
   - Emissão de alerta de risco crítico quando faltarem 30, 15 e 5 dias para o prazo fatal.

3. **Ciclo de Diligências da Mandatária (Caixa Econômica Federal - GIGOV)**:
   - Estados da condicionante: `PENDENTE`, `EM_ANALISE_CAIXA`, `DILIGENCIA_EMITIDA`, `APROVADO`.
   - Controle da data-limite de saneamento de pendências apontadas pela Caixa (`data_limite_saneamento`).
   - Armazenamento dos laudos técnicos e comprovantes no MinIO (`s3_key_documento`, `s3_key_laudo_pendencias`).

4. **Superação e Retirada da Cláusula Suspensiva**:
   - Quando todos os 3 pilares estiverem com status `APROVADO`, o sistema permite emitir e registrar o **Termo de Retirada da Cláusula Suspensiva** (`s3_key_termo_retirada_suspensiva`).
   - A superação da cláusula suspensiva conclui a Fase 2 e destrava a permissão para iniciar a **Fase 3 (Processos Licitatórios e Emissão de AIO)**.
   - Disparo de evento RabbitMQ `core.clausula-suspensiva.superada` na exchange `govflow.events`.

---

## 3. Arquitetura e Modelagem de Dados Existente

### 3.1 Tabelas no Banco de Dados (`core_schema`)

A infraestrutura relacional já está criada e versionada pelas migrações Flyway **`V3__complete_core_lifecycle_schema.sql`** e **`V7__enhance_fase_2_clausula_suspensiva_fields.sql`**:

```sql
-- 1. tb_convenios (campos enriquecidos em V7)
ALTER TABLE core_schema.tb_convenios
    ADD COLUMN IF NOT EXISTS prorrogacao_solicitada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS novo_prazo_prorrogado DATE,
    ADD COLUMN IF NOT EXISTS s3_key_termo_retirada_suspensiva VARCHAR(500);

-- 2. tb_condicionantes_suspensivas (V3 + V7)
CREATE TABLE IF NOT EXISTS core_schema.tb_condicionantes_suspensivas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    tipo_condicionante VARCHAR(50) NOT NULL, -- 'ENGENHARIA_PROJETOS_SINAPI', 'LICENCIAMENTO_AMBIENTAL', 'TITULARIDADE_IMOVEL'
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE', -- 'PENDENTE', 'EM_ANALISE_CAIXA', 'DILIGENCIA_EMITIDA', 'APROVADO'
    numero_documento_comprobatorio VARCHAR(100),
    data_aprovacao DATE,
    data_validade DATE,
    observacoes_analise_caixa TEXT,
    s3_key_documento VARCHAR(500),
    data_limite_saneamento DATE,
    s3_key_laudo_pendencias VARCHAR(500),
    valor_orcamento_aprovado_caixa NUMERIC(15, 2),
    percentual_bdi_aprovado NUMERIC(5, 2),
    numero_art_rrt VARCHAR(50),
    orgao_emissor VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_convenio_tipo_condicionante UNIQUE (convenio_id, tipo_condicionante)
);
```

---

## 4. Roteiro Passo a Passo Sugerido para a TASK-13

### Passo 1: Domínio e Modelo Rico (Hexagonal / DDD)
1. Criar enums em `br.com.govflow.core.domain.model.convenio`:
   - `TipoCondicionanteSuspensiva` (`ENGENHARIA_PROJETOS_SINAPI`, `LICENCIAMENTO_AMBIENTAL`, `TITULARIDADE_IMOVEL`).
   - `StatusCondicionanteSuspensiva` (`PENDENTE`, `EM_ANALISE_CAIXA`, `DILIGENCIA_EMITIDA`, `APROVADO`).
2. Criar entidade de domínio `CondicionanteSuspensiva`:
   - Regras de transição de estado.
   - Invariantes de preenchimento obrigatório para aprovação (SPA, Matrícula CRI, Licença Ambiental).
   - Validação de data-limite de saneamento quando em diligência.
3. Enriquecer `Convenio` no domínio:
   - Adicionar métodos de controle da cláusula suspensiva: `verificarSuperacaoSuspensiva()`, `solicitarProrrogacaoPrazo(LocalDate novoPrazo)`, `registrarTermoRetirada(String s3Key)`.
   - Propriedade indicando se a Cláusula Suspensiva está superada ou pendente.

### Passo 2: Portas de Repositório e Casos de Uso
1. Criar porta de repositório `CondicionanteSuspensivaRepositoryPort`:
   - `List<CondicionanteSuspensiva> buscarPorConvenioId(UUID convenioId, UUID tenantId)`
   - `Optional<CondicionanteSuspensiva> buscarPorConvenioETipo(UUID convenioId, TipoCondicionanteSuspensiva tipo, UUID tenantId)`
   - `CondicionanteSuspensiva salvar(CondicionanteSuspensiva condicionante)`
2. Criar UseCases:
   - `ConsultarCondicionantesUseCase`
   - `AtualizarCondicionanteUseCase`
   - `RegistrarDiligenciaCaixaUseCase`
   - `SuperarClausulaSuspensivaUseCase`
3. Criar serviço aplicacional `ClausulaSuspensivaService`:
   - Inicialização automática das 3 condicionantes padrão se o convênio estiver na Fase 2 e ainda não tiver registros.
   - Validação se os 3 pilares foram aprovados antes de permitir a emissão do Termo de Retirada.
   - Emissão de eventos RabbitMQ em caso de diligência iminente ou superação com sucesso.

### Passo 3: Adaptadores de Infraestrutura & JPA
1. Criar `CondicionanteSuspensivaJpaEntity` mapeada para `core_schema.tb_condicionantes_suspensivas` com `@TenantId`.
2. Criar `SpringDataCondicionanteSuspensivaRepository` e `CondicionanteSuspensivaRepositoryAdapter`.
3. Criar integração com MinIO / Armazenamento S3 (reaproveitando porta de upload ou adapter de armazenamento existente em `core-service`).

### Passo 4: Camada REST & Gateway
1. Criar DTOs de entrada e saída:
   - `CondicionanteSuspensivaResponse`
   - `DossieClausulaSuspensivaResponse` (contendo prazo fatal, dias restantes, lista dos 3 pilares e status geral)
   - `AtualizarCondicionanteRequest`
   - `RegistrarDiligenciaRequest`
   - `SolicitarProrrogacaoPrazoRequest`
2. Criar `ClausulaSuspensivaController` sob `/api/v1/convenios/{convenioId}/clausula-suspensiva/**`.
3. Registrar rota no `GatewayRoutesConfig` do `gateway` se necessário.

### Passo 5: Frontend Cockpit & Checklist Visual
1. Integrar com o modal da Fase 2 no `CockpitConvenioComponent` ou sub-aba de Gestão de Cláusula Suspensiva.
2. Renderizar checklist visual dos 3 pilares:
   - Card Engenharia (SPA, SINAPI, BDI, ART).
   - Card Ambiental (Licença / Dispensa).
   - Card Titularidade (CRI / Termo de Doação).
3. Botão de download/visualização dos documentos anexados.
4. Indicador de contagem regressiva de dias para o prazo fatal.

---

## 5. Diretriz Obrigatória Permanente: Protocolo de Fechamento de Task

> [!IMPORTANT]
> **Instrução Permanente para Todos os Agentes e Desenvolvedores:**  
> Nenhuma task deve ser dada como concluída apenas com a escrita do código inicial. Todo ciclo de entrega no ecossistema GovFlow **DEVE** obrigatoriamente cumprir as 5 etapas do **Protocolo de Fechamento de Task** antes de passar o bastão para a próxima demanda.

### As 5 Etapas do Fechamento de Task

1. **Revisão de Código & Alinhamento Crítico (`/code-review` + `/grill-me`)**:
   - Executar `/code-review` analisando Standards (Clean Architecture, DDD, SOLID, convenções do projeto) e Spec (critérios de aceite do backlog).
   - Submeter os apontamentos a uma deliberação estruturada com o desenvolvedor (`/grill-me` ou perguntas de alinhamento) para definir com clareza o que entra no plano de refatoração e o que é mantido com justificativa técnica.

2. **Refatoração & Garantia de Zero Regressões (TDD)**:
   - Elaborar o plano de refatoração (`/plan`) detalhando os ajustes acordados.
   - Executar as refatorações mantendo comunicação ativa (`transparent-pairing`).
   - Rodar a suíte completa de testes automatizados de **todos** os serviços do monorepo (`transferegov-service`, `core-service`, `whatsapp-service`, `gateway`, etc.), garantindo **100% de testes verdes (zero falhas e zero regressões)**.

3. **Deploy e Sincronização da Infraestrutura Docker**:
   - Se houver novas migrações Flyway no banco de dados (`flyway/sql`), aplicar via container:
     ```bash
     docker compose up flyway
     ```
   - Reconstruir e subir os containers dos microsserviços alterados para garantir paridade exata entre ambiente local e containerizado:
     ```bash
     docker compose up -d --build <servico-alterado>
     ```
   - Verificar a saúde e status de execução dos containers via `docker compose ps` e smoke test.

4. **Atualização da Documentação & Handoff**:
   - Marcar todos os critérios de aceite concluídos em [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md).
   - Gerar ou atualizar o arquivo [`docs/handoff_task_XX.md`](file:///c:/projetos/estudo%20spring/govflow/docs/) para a próxima task, contendo o estado atualizado do repositório, métricas de testes, diagramas e guia passo a passo da nova fase.
   - Replicar este bloco de instrução no novo documento de handoff.

5. **Versionamento Semântico e Publicação no GitHub**:
   - Verificar branch atual (`git branch --show-current`).
   - Gerar commit padronizado seguindo a convenção Sentry / Conventional Commits (`feat(...)`, `fix(...)`, `ref(...)`) com escopo e descrição clara do que foi entregue.
   - Publicar no GitHub via `git push origin <branch>`.
