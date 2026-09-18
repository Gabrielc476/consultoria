# Handoff: Transição para TASK-07 — Frontend Angular (Tela de Conferência Lado a Lado / Side-by-Side Review)

## 1. Resumo Executivo & Estado Atual do Repositório

- **Última Entrega Concluída (TASK-06 — Core Service):**
  - **Agregado `Documento` e Ciclo de Aprovação Auditada implementados:**
    - Ciclo de estados estrito: `RECEBIDO` -> `EM_CONFERENCIA` -> `PRONTO_PARA_TRANSFEREGOV` (ou `REJEITADO`).
    - Cálculo determinístico de *diff* (`DiffRevisao`) comparando sugestão da IA e dados aprovados.
    - Trilha imutável em `tb_auditorias_revisao` e persistência multi-tenant (`@TenantId`) em `tb_documentos` (Flyway `V15`).
    - Validação de consistência fiscal matemática (tolerância de R$ 0,02).
    - Listener AMQP `DocumentoProcessadoListener` consumindo `DocumentoExtraidoEvent` da fila `fila.documentos.processados`.
    - Publisher `RabbitMQDocumentoEventPublisherAdapter` emitindo `DocumentoProntoParaTransferegovEvent` e `DocumentoRejeitadoEvent` na exchange `govflow.events`.
    - Endpoints REST implementados no `DocumentoController`:
      - `GET /api/v1/documentos/{id}`: Retorna documento consolidado com sugestões e `boundingBoxes`.
      - `GET /api/v1/documentos`: Listagem paginada com filtro por `status`.
      - `PUT /api/v1/documentos/{id}/aprovar`: Payload de revisão e aprovação.
      - `PUT /api/v1/documentos/{id}/rejeitar`: Rejeição com motivo obrigatório.
      - `GET /api/v1/documentos/{id}/auditoria`: Histórico com diff.
    - **100% dos 78 testes automatizados do `core-service` passando com sucesso.**

---

## 2. Foco da Próxima Sessão: [TASK-07]

### Objetivo da TASK-07
Implementar no módulo frontend web SPA (Angular 19/20 utilizando Feature-Sliced Design e Signals) a **Tela de Conferência Lado a Lado (Side-by-Side Review)**:
1. **Lado Esquerdo (50% da viewport):**
   - Visualizador de PDF/imagem (`ngx-extended-pdf-viewer`) com zoom, rotação e paginação.
   - Camada overlay renderizando retângulos sobre as **bounding boxes** onde a IA identificou cada dado.
2. **Lado Direito (50% da viewport):**
   - Formulário reativo baseado em Signals com os campos fiscais pré-preenchidos.
   - Semáforo de confiança por campo (Verde: >90%, Amarelo: 70-90%, Vermelho: <70%).
   - Recálculo dinâmico e em tempo real do líquido e deduções tributárias.
3. **Ações:**
   - Botão **Aprovar**: Invoca `PUT /api/v1/documentos/{id}/aprovar` no Core Service e avança para o próximo pendente.
   - Botão **Rejeitar**: Abre modal para justificativa e invoca `PUT /api/v1/documentos/{id}/rejeitar`.

---

## 3. Artefatos de Referência no Repositório

- **Especificação da TASK-07:** [docs/clickup_tasks_backlog.md](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L124-L138)
- **Documentação de Endpoints do Core Service:** [services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/DocumentoController.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/DocumentoController.java)
- **Contratos de Response do Documento:** [services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/dto/response/DocumentoResponse.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/dto/response/DocumentoResponse.java)
- **Arquitetura Frontend:** [docs/architecture/06_arquitetura_frontend_e_extensao.md](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/06_arquitetura_frontend_e_extensao.md)

---

## 4. Suggested Skills para o Próximo Agente

1. **`angular`**: Standalone components, Zoneless, Signals e Angular 19/20.
2. **`angular-ui-patterns`**: Padrões de loading, erro e side-by-side rendering.
3. **`frontend-design`**: UI/UX de alta produtividade para conferência fiscal em menos de 30 segundos.
4. **`commit`**: Commits convencionais e rastreamento de entregas.
