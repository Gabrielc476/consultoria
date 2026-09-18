# Handoff: Transição para TASK-06 — Core Service (Agregado Documento e Ciclo de Aprovação Auditada)

## 1. Resumo Executivo & Estado Atual do Repositório

- **Branch Atual:** `main` (sincronizada com `origin/main` no commit `5f747a8`).
- **Últimas Entregas Concluídas (TASK-05 e Refinamento Resiliente):**
  - **`ai-service` (Python/FastAPI):**
    - Extração fiscal multimodal implementada com suporte primário ao **Gemma 4** (`gemma-4-31b-it`) e fallback em cascata para **Gemini 3.7 Flash** e **Gemini 3.1 Flash Lite**.
    - Prompt blindado com diretrizes estritas de saída e regras campo a campo.
    - Sanitização robusta com regex para isolamento de JSON e coerção permissiva de tipos no Pydantic (`documento_habil_schema.py`).
    - Todos os 15 testes unitários passando com 100% de sucesso.
    - Container `govflow-ai-service` reconstruído e operando com status `Up (healthy)` na porta `8000`.
  - **`whatsapp-service` (Java 21 / Spring Boot 3.3):**
    - Suporte a decodificação e streaming de mídias em base64 da Evolution API no `EvolutionWebhookParser` e `MediaStorageHandler`.
    - 6 testes unitários do parser validados com sucesso.
  - **Mensageria RabbitMQ:**
    - O `ai-service` consome da fila `fila.documentos.extrair` e publica eventos tipados `DocumentoExtraidoEvent` na exchange `govflow.events` com routing key `documento.extraido` (fila: `fila.documentos.processados`).

---

## 2. Foco da Próxima Sessão: [TASK-06]

### Objetivo da TASK-06
Implementar no `services/core-service` o **Agregado Documento** e o **Ciclo de Aprovação Auditada (Human-in-the-Loop)**, seguindo estritamente a **Arquitetura Hexagonal (Ports & Adapters)** e **Domain-Driven Design (DDD)** já estabelecidos no módulo.

### Escopo e Entregáveis da TASK-06:
1. **Domínio Puro (Domain Layer - Zero Dependências de Framework):**
   - Agregado `Documento` com ciclo de vida e transições de estado:
     - Estados: `RECEBIDO` -> `EM_CONFERENCIA` -> `PRONTO_PARA_TRANSFEREGOV` (ou `REJEITADO`).
   - Value Objects:
     - `ExtracaoSugerida`: Dados fiscais lidos pela IA (número, data, CNPJ, razão social, bruto, líquido, retenções) com score de confiança geral e por campo.
     - `BoundingBoxesData`: Metadados das coordenadas normalizadas `[ymin, xmin, ymax, xmax]` para renderização no frontend.
     - `AuditoriaRevisao`: Registro do analista responsável, carimbo de data/hora, justificativa e o **diff** (comparativo entre os valores originais da IA e os valores corrigidos manualmente).
   - Invariantes de Domínio:
     - Bloqueio de aprovação se campos obrigatórios estiverem ausentes.
     - Cálculo determinístico de consistência fiscal no aceite final.
2. **Portas e Casos de Uso (Application Layer):**
   - Inbound Ports (Use Cases):
     - `ProcessarDocumentoExtraidoUseCase`: Processa o evento da IA e transiciona o documento para `EM_CONFERENCIA`.
     - `ConsultarDocumentoUseCase`: Retorna os dados completos do documento, incluindo sugestões da IA e coordenadas para a tela de conferência.
     - `AprovarDocumentoUseCase`: Recebe a revisão final do analista, registra a auditoria com diff e transiciona para `PRONTO_PARA_TRANSFEREGOV`.
     - `RejeitarDocumentoUseCase`: Transiciona o documento para `REJEITADO` com motivo obrigatório.
   - Outbound Ports:
     - `DocumentoRepositoryPort`: Persistência e busca multi-tenant.
     - `AuditoriaRevisaoRepositoryPort`: Persistência da trilha de auditoria.
     - `DocumentoEventPublisherPort`: Publicação de evento de documento pronto para submissão no Transferegov.
3. **Adaptadores Externos (Infrastructure Layer):**
   - **Inbound AMQP Listener:**
     - `DocumentoProcessadoListener`: Escuta a fila `fila.documentos.processados`, consome `DocumentoExtraidoEvent` e invoca `ProcessarDocumentoExtraidoUseCase`.
   - **Inbound REST Controller:**
     - `GET /api/v1/documentos/{id}`: Retorna o documento consolidado para a interface side-by-side (TASK-07).
     - `PUT /api/v1/documentos/{id}/aprovar`: Endpoint para aprovação com payload de revisão e persistência de auditoria.
     - `PUT /api/v1/documentos/{id}/rejeitar`: Endpoint para rejeição fundamentada.
   - **Outbound Persistence Adapter:**
     - Entidades JPA (`DocumentoJpaEntity`, `AuditoriaRevisaoJpaEntity`) mapeadas no `core_schema` com suporte multi-tenant (`@TenantId`).
     - Migrations Flyway correspondentes (ex: `V15__create_documentos_and_auditoria_tables.sql`).

---

## 3. Artefatos de Referência no Repositório

- **Especificação Detalhada da TASK-06:** [docs/clickup_tasks_backlog.md](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L107-L122)
- **Arquitetura Hexagonal do Core Service:** [docs/architecture/02_arquitetura_core_service_hexagonal.md](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/02_arquitetura_core_service_hexagonal.md)
- **Modelo de Domínio e Entidades:** [docs/architecture/09_modelo_de_dominio_e_entidades.md](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/09_modelo_de_dominio_e_entidades.md)
- **Contrato de Eventos do AI Service:** [services/ai-service/src/domain/schemas/events.py](file:///c:/projetos/estudo%20spring/govflow/services/ai-service/src/domain/schemas/events.py#L186-L212)
- **Histórico de Auditorias e Aceite TASK-05:** [docs/audits/test_results.json](file:///c:/projetos/estudo%20spring/govflow/docs/audits/test_results.json#L67-L171)

---

## 4. Suggested Skills para o Próximo Agente

Ao iniciar a TASK-06, o novo agente deve ativar e seguir as diretrizes das seguintes skills:

1. **`java-pro`**: Boas práticas de Java 21 (Records, Pattern Matching, Sealed Interfaces) e Spring Boot 3.3.
2. **`ddd-tactical-patterns`**: Modelagem tática do Agregado `Documento`, Value Objects imutáveis e aplicação de invariantes de negócio sem acoplamento a frameworks.
3. **`clean-code`**: Código limpo, nomenclatura expressiva e separação estrita de camadas na Arquitetura Hexagonal.
4. **`commit`**: Padronização dos commits no formato convencional/Sentry ao concluir cada etapa.

---

## 5. Comandos Úteis de Inicialização e Validação

```bash
# 1. Verificar status dos containers Docker
docker compose ps

# 2. Executar suíte de testes do core-service
mvn test -f services/core-service/pom.xml

# 3. Executar suíte de testes do ai-service
python -m pytest services/ai-service/tests

# 4. Inspecionar fila de documentos processados no RabbitMQ
# Painel web: http://localhost:15672 (login: govflow / senha configurada no .env)
# Fila alvo: fila.documentos.processados
```
