# Handoff: Contexto e Guia para Execução da TASK-12

> **Destino:** Próximo Agente Antigravity / Desenvolvedor  
> **Foco:** `[TASK-12] Radar CAUC: Monitoramento das 16 Exigências Fiscais (Fase 0)`  
> **Data de Geração:** 2026-09-25  
> **Status do Repositório:** 179/179 testes de backend passando (100% verde) + 31/31 testes de frontend Angular passando (100% verde).

---

## 1. Estado Atual do Ecossistema GovFlow (Após Conclusão da TASK-11)

O ecossistema GovFlow concluiu a ingestão integral do ecossistema federal do Transferegov com 100% de cobertura e estabilidade:

- **`transferegov-service` (:8082)**:
  - **TASK-09**: Pipeline de streaming ETL em memória (Zero Disk I/O) para dados diários do SICONV, Motor Data Quality de 6 dimensões, quarentena de anomalias em `tb_sincronizacao_anomalias` e persistência de convênios em `tb_sincronizacao_convenio`.
  - **TASK-10**: Motor de cálculo e monitoramento de criticidade de prazos (`DeadlineDetectorService`), avaliação de marcos temporais (`CLAUSULA_SUSPENSIVA`, `FIM_VIGENCIA`, `PRESTACAO_CONTAS`), semáforo de risco (`CRITICO`, `ATENCAO`, `REGULAR`), emissão de eventos RabbitMQ na exchange `govflow.events` com routing key `transferegov.prazo.alerta` e agendamento matinal (`DeadlineMonitoringScheduler`).
  - **TASK-11**: Ingestão da API REST de Emendas Especiais (Emendas Pix) `/especiais` com `RestClient` resiliente (connection pooling via `JdkClientHttpRequestFactory`), retries e rate limiting. Motor de compliance ADPF 854 / Portaria MGI nº 33/2023 (`Adpf854ComplianceEvaluator`), persistência relacional com Flyway `V17`, emissão de eventos RabbitMQ (`transferegov.emenda.adpf854.alerta`) sem truncamento para inconformidades críticas e alertas, endpoints REST de consulta CQRS-Light segregados (`/api/v1/transferegov/emendas-especiais/**`) e trigger manual isolado em `EmendaEspecialSyncController`.
- **`core-service` (:8081)**:
  - Multi-tenancy isolado via schemas PostgreSQL (`core_schema`), gestão de prefeituras, consultorias e convênios base.
  - Tabela `core_schema.tb_certidoes_cauc` já provisionada na migration Flyway `V3__complete_core_lifecycle_schema.sql`.
- **`frontend` (Angular 19+ Standalone)**:
  - Página `/radar-cauc` e componente visual `cauc-health-matrix` (`frontend/src/app/features/radar-cauc/`) já prototipados e implementados na `[TASK-FE-06]`.
- **Suíte de Testes Geral**:
  - `transferegov-service`: 48 testes (0 falhas)
  - `core-service`: 94 testes (0 falhas)
  - `gateway`: 21 testes (0 falhas)
  - `whatsapp-service`: 16 testes (0 falhas)
  - **Total Backend**: 179 testes automatizados passando (100% verde).
  - **Total Frontend**: 31 testes automatizados passando (100% verde).

---

## 2. Escopo e Especificação da TASK-12

### 2.1 Identificação
* **Tarefa:** `[TASK-12] Radar CAUC: Monitoramento das 16 Exigências Fiscais (Fase 0)`
* **Localização no Backlog:** [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L316-L329)
* **Serviços Afetados:** `core-service` (Backend) e integração com o `frontend` (`features/radar-cauc`).
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-03]`, Flyway `V3` / `V5`.
* **Fundamentação Legal:** Art. 25 da Lei de Responsabilidade Fiscal (LC nº 101/2000), Instrução Normativa STN nº 01/2021 e regras de celebração de convênios da Portaria Conjunta MGI/MF/CGU nº 33/2023.

### 2.2 O Que a Task Entrega
1. **Modelo de Domínio & Persistência das Certidões:**
   - Mapeamento JPA para a tabela `core_schema.tb_certidoes_cauc` (já existente no schema Flyway `V3`).
   - Seção para as **16 certidões fiscais e orçamentárias obrigatórias** distribuídas em 4 grupos:
     - **Grupo I (Obrigações Financeiras):**
       - 1.1 Receita Federal e PGFN (Certidão Conjunta Negativa de Débitos)
       - 1.2 Regularidade do FGTS (CRF Caixa)
       - 1.3 Regularidade Previdenciária (RPPS ou Regime Geral)
       - 1.4 Débitos Trabalhistas (CNDT)
     - **Grupo II (Adimplência Financeira):**
       - 2.1 Prestação de Contas de Recursos Federais Recebidos (SIAFI / Transferegov)
       - 2.2 CADIN Federal
       - 2.3 Regularidade perante a Previdência Social
     - **Grupo III (Prestação de Contas / Transparência Fiscal):**
       - 3.1 Relatório Resumido da Execução Orçamentária (RREO - SICONFI)
       - 3.2 Relatório de Gestão Fiscal (RGF - SICONFI)
       - 3.3 Homologação do Balanço Anual no SICONFI
       - 3.4 Encaminhamento das Contas Anuais ao TCE
     - **Grupo IV (Limites Constitucionais e Legais):**
       - 4.1 Aplicação Mínima em Ações e Serviços Públicos de Saúde (15% - SIOPS)
       - 4.2 Aplicação Mínima em Manutenção e Desenvolvimento do Ensino (25% - SIOPE)
       - 4.3 Despesa Total com Pessoal (Limite LRF 54% - SICONFI)
       - 4.4 Dívida Consolidada Líquida dentro do limite
       - 4.5 Operações de Crédito e ARO (Antecipação de Receita Orçamentária)
2. **Motor de Monitoramento e Semáforo de Risco:**
   - Avaliação contínua da proximidade de expiração de certidões:
     - `REGULAR`: Mais de 10 dias para o vencimento.
     - `ALERTA` / `EM_RISCO`: Faltando 10 ou 5 dias para o vencimento (janela crítica para renovação antes de travar repasses).
     - `IRREGULAR` / `VENCIDA`: Certidão expirada ou com restrição ativa.
   - Atualização automática do campo `status_cauc` da `tb_prefeituras` (`ADIMPLENTE` vs `BLOQUEADO`).
3. **Disparo de Eventos & Alertas:**
   - Emissão de eventos via RabbitMQ na exchange `govflow.events` com routing key `core.cauc.alerta` quando certidões entrarem em estado de alerta ou vencimento.
   - Integração com o módulo de notificações do WhatsApp para avisar secretários de finanças e controladores internos do município.
4. **Camada REST / APIs:**
   - Endpoints sob `/api/v1/cauc`:
     - `GET /api/v1/cauc/prefeituras/{prefeituraId}`: Dossiê completo das 16 certidões com status, data de emissão, validade e dias restantes.
     - `GET /api/v1/cauc/resumo`: Matriz agregada para todas as prefeituras gerenciadas pela consultoria (compatível com `cauc.model.ts` e `cauc-health-matrix`).
     - `POST /api/v1/cauc/prefeituras/{prefeituraId}/certidoes`: Cadastro ou atualização manual de certidão / upload de comprovante PDF.
     - `POST /api/v1/cauc/avaliar`: Trigger sob demanda para reavaliação de conformidade e atualização de semáforos.

---

## 3. Arquitetura e Referência Técnica

### 3.1 Tabela `core_schema.tb_certidoes_cauc` (Flyway V3)
```sql
CREATE TABLE IF NOT EXISTS core_schema.tb_certidoes_cauc (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prefeitura_id UUID NOT NULL REFERENCES core_schema.tb_prefeituras(id) ON DELETE CASCADE,
    tipo_exigencia VARCHAR(50) NOT NULL,
    numero_certidao VARCHAR(100),
    data_emissao DATE NOT NULL,
    data_validade DATE NOT NULL,
    situacao VARCHAR(30) NOT NULL DEFAULT 'REGULAR', -- 'REGULAR', 'IRREGULAR', 'EM_RISCO'
    dias_para_vencer INTEGER,
    s3_key_comprovante VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prefeitura_tipo_certidao UNIQUE (prefeitura_id, tipo_exigencia)
);
```

### 3.2 Contrato com o Frontend (`frontend/src/app/features/radar-cauc/model/cauc.model.ts`)
```typescript
export type StatusCertidao = 'REGULAR' | 'ALERTA' | 'VENCIDA';

export interface CertidaoCaucItem {
  codigo: string; // Ex: '1.1', '1.2'
  grupo: 'TRIBUTOS_FGTS' | 'PRESTACAO_CONTAS' | 'SICONFI_FISCAL' | 'LIMITES_CONSTITUCIONAIS';
  nome: string;
  orgaoEmissor: string;
  status: StatusCertidao;
  dataValidade: string;
  diasParaVencer: number;
}
```

---

## 4. Roteiro Passo a Passo Sugerido para a TASK-12

1. **Assunção & Context Load**:
   - Inspecionar a entidade `PrefeituraEntity` em `core-service`.
   - Verificar enum `StatusCauc` (`ADIMPLENTE`, `BLOQUEADO`).
2. **Entidades & Repositórios (JPA)**:
   - Criar `CertidaoCaucEntity` mapeando `tb_certidoes_cauc`.
   - Criar enum `TipoExigenciaCauc` (com os 16 itens oficiais, códigos 1.1 a 4.5 e grupos LRF).
   - Criar `CertidaoCaucRepository` com buscas por prefeitura, tenant e status.
3. **Serviços de Negócio & Compliance**:
   - Criar `CaucMonitorService`:
     - Calcular `diasParaVencer` com base na data atual (`LocalDate.now()`).
     - Determinar semáforo (`REGULAR`, `ALERTA`, `VENCIDA`).
     - Atualizar o status geral da prefeitura se qualquer certidão crítica estiver vencida.
   - Criar `CaucEventPublisher` para publicar alertas em `core.cauc.alerta`.
   - Criar scheduler diário `CaucDailyScheduler` para varredura matinal.
4. **Controllers & DTOs**:
   - Criar `CaucController` com os endpoints REST sob `/api/v1/cauc`.
   - Mapear DTOs exatamente alinhados com o frontend (`cauc.model.ts`).
5. **Testes & Validação**:
   - Escrever testes unitários e de integração (MockMvc) no `core-service`.
   - Executar regressão completa garantindo que o backend continue 100% verde (179+ testes).

---

## 5. Diretriz Obrigatória para Handoffs Futuros: Protocolo de Fechamento de Task

> [!IMPORTANT]
> **Instrução Permanente para Todos os Agentes e Desenvolvedores:**  
> Nenhuma task deve ser dada como concluída apenas com a escrita do código inicial. Todo ciclo de entrega no ecossistema GovFlow **DEVE** obrigatoriamente cumprir as 5 etapas do **Protocolo de Fechamento de Task** antes de passar o bastão para a próxima demanda.

### 5.1 As 5 Etapas do Fechamento de Task

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
   - Verificar a saúde e status de execução dos containers via `docker compose ps`.

4. **Atualização da Documentação & Handoff**:
   - Marcar todos os critérios de aceite concluídos em [`docs/clickup_tasks_backlog.md`](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md).
   - Gerar ou atualizar o arquivo [`docs/handoff_task_XX.md`](file:///c:/projetos/estudo%20spring/govflow/docs/) para a próxima task, contendo o estado atualizado do repositório, métricas de testes, diagramas e guia passo a passo da nova fase.
   - Replicar este bloco de instrução no novo documento de handoff.

5. **Versionamento Semântico e Publicação no GitHub**:
   - Verificar branch atual (`git branch --show-current`).
   - Gerar commit padronizado seguindo a convenção Sentry / Conventional Commits (`feat(...)`, `fix(...)`, `ref(...)`) com escopo e descrição clara do que foi entregue.
   - Publicar no GitHub via `git push origin <branch>`.

