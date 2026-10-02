# Relatório de Sessão — Implementação do EPIC 2: Módulo 2 — GED & Ficheiro Digital do Convênio

**Data:** 29 de Setembro de 2026 / 30 de Setembro de 2026  
**Status do Módulo:** 100% Concluído e Validado  
**Versão do Sistema:** GovFlow v0.2.0-SNAPSHOT  
**Skills Ativas:** `[govflow-architecture-sync, java-pro, architecture-patterns, database-migrations-sql-migrations, angular, angular-best-practices, tdd, transparent-pairing, clean-code]`

---

## 1. Sumário Executivo

O **EPIC 2 (Módulo 2 — GED & Ficheiro Digital do Convênio)** foi implementado integralmente de ponta a ponta. O objetivo primordial deste módulo foi alcançado: **substituir definitivamente o sistema arcaico e disperso de pastas locais do Windows das consultorias por um repositório inteligente no MinIO/S3, estruturado e organizado de forma determinística nas 10 Fases Oficiais do Ciclo de Vida da Transferência Voluntária**.

Todos os 8 tickets do backlog (`TICKET-GED-01` ao `TICKET-GED-08`) foram executados, testados com cobertura unitária e de integração automatizada, integrados ao API Gateway e compilados sem erros no frontend Angular reativo com Signals.

---

## 2. Conformidade com os Guardrails e Diretrizes Técnicas

| Guardrail Mandatório | Status | Evidência Técnica de Implementação |
| :--- | :---: | :--- |
| **1. Arquitetura Hexagonal Pura** | ✅ Conforme | Entidades de domínio (`Documento`, `FicheiroDigital`, `DocumentoAuditoria`, `DocumentoHabilDados`) e enums (`FaseCicloVida`, `CategoriaDocumento`, `OrigemCanal`) residem em `domain/model` livres de qualquer anotação JPA (`@Entity`) ou Jackson (`@JsonProperty`). O acoplamento relacional e de persistência reside estritamente em `infrastructure/adapter/out/persistence`. |
| **2. Isolamento Multi-Tenant & Escopo de Prefeitura** | ✅ Conforme | Operações de leitura, upload, movimentação e download ZIP validam o `tenant_id` via `UserContext` e, para usuários `AGENTE`, validam `UserContext.temAcessoPrefeitura(prefeituraId)`. Acessos não autorizados respondem HTTP 403 Forbidden (`AcessoNegadoException`). |
| **3. Roteamento no Gateway sem Erro 404** | ✅ Conforme | Mapeado em `GatewayRoutesConfig.java` com a rota `core-ficheiro` para `/api/v1/convenios/*/ficheiro/**` e `core-documentos` para `/api/v1/documentos/**`, preservando os cabeçalhos de contexto `X-Tenant-Id`, `X-User-Id` e `X-User-Roles`. |
| **4. Jackson Serialization Safe** | ✅ Conforme | DTOs e Records utilizam representações imutáveis explícitas sem métodos virtuais computados sem `@JsonIgnore`, prevenindo erros de desserialização em runtime. |
| **5. Storage Hierárquico MinIO/S3 + SHA-256 + Presigned URL** | ✅ Conforme | Convenção estrita de chaves S3: `tenants/{tenantId}/prefeituras/{prefeituraId}/convenios/{convenioId}/fases/{fase}/{docId}_{nomeOriginal}`. Cálculo simultâneo de hash SHA-256 no stream de bytes e emissão de URLs pré-assinadas com validade de 15 minutos via `S3Presigner` para o endpoint `/preview`. |
| **6. Streaming ZIP Não Bloqueante (Zero Heap Blowup)** | ✅ Conforme | `ExportarFicheiroZipService` utiliza `ZipOutputStream` sobre `StreamingResponseBody`, compactando arquivos organizados fielmente na árvore `/SICONV_.../fase/arquivo` diretamente para o `OutputStream` da resposta HTTP sem buffering na memória Heap da JVM. |
| **7. Zero Mocks no Frontend** | ✅ Conforme | Remoção completa de `MOCK_DOCS` em `convenio-cockpit-page.component.ts`. O novo componente `FicheiroDigitalComponent` e o modal `DocumentoPreviewModalComponent` operam 100% integrados à API real reativa via Signals. |

---

## 3. Detalhamento dos Tickets Entregues

### [TICKET-GED-01] Migração de Banco de Dados: Generalização de Documentos
- **Arquivo:** `flyway/sql/V20__generalize_documentos_and_create_ficheiro_tables.sql`
- **Ações:**
  - Adição de colunas estruturais em `core_schema.tb_documentos`: `fase_ciclo_vida`, `categoria_documento`, `pasta_virtual`, `tags`, `hash_sha256`, `metadados_json`, `origem_canal`, `criado_por_usuario_id`.
  - Criação da tabela satélite `core_schema.tb_documentos_habeis_dados` com migração dos dados fiscais legados (chave NF-e, credor, valores brutos/líquidos).
  - Criação da tabela imutável `core_schema.tb_documentos_auditoria` com índices compostos por documento, tenant e data.

### [TICKET-GED-02] Domínio e Portas Hexagonais do Ficheiro Digital
- **Entidades de Domínio:**
  - `FaseCicloVida.java`: As 10 fases oficiais numeradas de 00 a 09, com nomes padronizados de pastas físicas e descrições legais.
  - `CategoriaDocumento.java`: Taxonomia arquivística formal associada por padrão às fases do ciclo.
  - `OrigemCanal.java`: `UPLOAD_MANUAL`, `WHATSAPP`, `TRANSFEREGOV_CRAWLER`, `EMAIL`.
  - `FicheiroDigital.java`: Agregado com a árvore das 10 fases, contadores e totalizadores de bytes.
  - `DocumentoAuditoria.java`: Entidade pura de auditoria.
  - `DocumentoHabilDados.java`: Entidade pura para especialização fiscal.
- **Portas e Adaptadores:**
  - `DocumentoRepositoryPort.java` e `FicheiroRepositoryPort.java`.
  - `MinioDocumentoStorageAdapter.java` implementando `DocumentoStoragePort`.
  - `DocumentoPersistenceMapper.java`, `DocumentoJpaEntity.java`, `DocumentoHabilDadosJpaEntity.java` e `DocumentoAuditoriaJpaEntity.java`.

### [TICKET-GED-03] Backend Storage MinIO/S3 & Presigned URLs
- **Configuração:** `S3ClientConfig.java` expondo o bean `S3Presigner`.
- **Adaptador:** `MinioDocumentoStorageAdapter` calculando hash SHA-256 via `DigestInputStream` durante o upload no bucket `govflow-docs` e gerando presigned URLs com expiração configurada em 15 minutos para renderização inline de PDFs e imagens.

### [TICKET-GED-04] Engine de Download em Lote (Exportação ZIP)
- **Caso de Uso:** `ExportarFicheiroZipUseCase.java`.
- **Serviço:** `ExportarFicheiroZipService.java`.
- **Comportamento:** Compacta o convênio integral ou uma fase específica via streaming, gerando nomes padronizados (`SICONV_{numero}/{nomePasta}/{nomeArquivo}`) e descartando documentos com status `EXCLUIDO`.

### [TICKET-GED-05] Listener RabbitMQ de Documentos Classificados pela IA
- **Configuração:** `RabbitMQCoreConfig.java` com a fila `fila.documentos.classificados` e binding para `documento.classificado`.
- **Listener:** `DocumentoClassificadoListener.java`.
- **Regra de Negócio:**
  - Score > 0.90: arquivamento direto na pasta virtual da fase e registro de auditoria `CLASSIFICACAO_IA`.
  - Score <= 0.90: encaminhamento para a pasta `/triagem` e registro de auditoria `ENCAMINHADO_TRIAGEM`.

### [TICKET-GED-06] REST API: Endpoints do Ficheiro Digital e Gateway
- **Controlador:** `FicheiroDigitalController.java`.
- **Endpoints Expostos:**
  - `GET /api/v1/convenios/{convenioId}/ficheiro`
  - `GET /api/v1/convenios/{convenioId}/ficheiro/fases/{fase}`
  - `POST /api/v1/convenios/{convenioId}/ficheiro/upload` (Multipart)
  - `GET /api/v1/convenios/{convenioId}/ficheiro/download-zip` (Streaming ZIP integral)
  - `GET /api/v1/convenios/{convenioId}/ficheiro/fases/{fase}/download-zip` (Streaming ZIP da fase)
  - `GET /api/v1/documentos/{id}/preview` (Presigned URL)
  - `PATCH /api/v1/documentos/{id}/mover` (Mover de pasta/fase com justificativa)
  - `DELETE /api/v1/documentos/{id}` (Soft delete com justificativa)
  - `GET /api/v1/documentos/{id}/historico-auditoria` (Trilha imutável)
- **Gateway:** Mapeado em `GatewayRoutesConfig.java` com preservação dos cabeçalhos multi-tenant.

### [TICKET-GED-07] Frontend Angular: Componente Explorer do Ficheiro Digital
- **Rota:** `/convenios/:id/ficheiro`.
- **Componente:** `FicheiroDigitalComponent.ts`.
- **Recursos:**
  - Painel lateral com as 10 Fases numeradas, contadores em tempo real e badges.
  - Tabela detalhada de documentos com ícones por extensão, categoria oficial, tamanho formatado e tags.
  - Modal de upload com área de Drag-and-Drop, seleção de categoria filtrada por fase e subpasta virtual.
  - Botões de download em lote (ZIP integral e ZIP por fase).

### [TICKET-GED-08] Frontend Angular: Visualizador Rápido Inline
- **Componente:** `DocumentoPreviewModalComponent.ts`.
- **Recursos:**
  - Renderizador inline de PDFs via `<iframe>` e imagens via `<img>` usando presigned URLs seguras.
  - Normalização automática de hostname (`http://localhost:9000`) e suporte a streaming direto como fallback garantido.
  - Drawer retrátil com abas de **Metadados & Integridade** (com cópia do Hash SHA-256), **Mover / Ajustar** (com seletor de fase e justificativa obrigatória) e **Auditoria** (trilha cronológica de ações).
  - Fluxo seguro de exclusão lógica com confirmação e justificativa.

---

## 4. Métricas de Testes e Validação Automatizada

### Core Service (Backend)
- **Comando:** `mvn test -f services/core-service/pom.xml`
- **Total de Testes:** **208 testes executados**
- **Falhas:** **0**
- **Erros:** **0**
- **Destaque:** Cobertura completa de testes unitários e de integração para `FicheiroDigitalTest`, `FicheiroDigitalServiceTest`, `ExportarFicheiroZipServiceTest`, `DocumentoClassificadoListenerTest`, `FicheiroDigitalControllerTest` e `MultiTenantIsolationIntegrationTest`.

### Gateway (Roteamento & Segurança)
- **Comando:** `mvn test -f gateway/pom.xml`
- **Total de Testes:** **21 testes executados**
- **Falhas:** **0**
- **Erros:** **0**

### Frontend (Compilação de Produção)
- **Comando:** `npm run build` (em `frontend/`)
- **Resultado:** **Build concluído com sucesso (10.4s)**
- **Erros:** **0**
- **Chunk Inicial:** 1.78 MB (290.62 kB transfer size)
- **Validação Visual:** Confirmada renderização nativa de PDF inline, cálculo de integridade SHA-256 e gaveta retrátil de metadados em ambiente real.

---

## 5. Evidência de Validação Visual na Interface Real (Frontend)

O usuário realizou a validação interativa no navegador em `http://localhost:4200/convenios/d476d220-9ba7-498c-9eef-49b02d5e2452/ficheiro`:
1. **Explorer das 10 Fases:** Árvore lateral operando com contadores de arquivos reais da prefeitura ativa (Patos - PB) e convênio `#914250/2023`.
2. **Visualizador Inline de PDF:** Modal aberto renderizando o leitor de PDF nativo com zoom, paginação, rotação e controles de impressão sem forçar download local.
3. **Painel Retrátil de Metadados e Integridade:**
   - Hash SHA-256 (`6a70638db848...`) com botão de cópia rápida.
   - Canal de Origem (`UPLOAD_MANUAL`).
   - Pasta Virtual (`01_Celebracao`).
   - MIME Type (`application/pdf`).
   - Data de Registro (`30/09/2026 10:14`).
   - Abas operacionais: *Metadados*, *Mover / Ajustar* e *Auditoria*.
4. **Conteúdo Real do Documento:** Renderização do documento institucional oficial do convênio com cabeçalho do Ministério das Cidades, SICONV, dotação orçamentária e autenticidade garantida.

---

## 6. Lições Aprendidas & Diagnósticos Críticos de Infraestrutura

### 6.1 Sincronização de Containers Docker vs. Builds Locais
- **Cenário:** Modificações nos arquivos Java e Angular não se refletiam no navegador porque os containers estavam executando imagens construídas antes das alterações.
- **Resolução:** Estabelecido fluxo estrito de atualização contínua:
  - `mvn package -DskipTests` + `docker cp target/*.jar govflow-core-service:/app/` + `docker restart`.
  - `npm run build` + `docker cp dist/frontend/browser/. govflow-frontend:/usr/share/nginx/html/` + `docker exec govflow-frontend nginx -s reload`.

### 6.2 Presigned URLs em Topologia Híbrida (Docker Network vs. Host Windows)
- **Problema:** O `core-service` roda dentro da rede Docker e se conecta ao MinIO via `http://minio:9000`. Ao gerar uma presigned URL via AWS S3 SDK (`S3Presigner`), o cabeçalho `Host: minio:9000` é assinado (`X-Amz-SignedHeaders=host`). Quando o navegador no Windows acessa `http://localhost:9000`, o cabeçalho enviado é `Host: localhost:9000`, provocando `403 Forbidden` (`SignatureDoesNotMatch`).
- **Resolução Arquitetural:**
  1. Configuração de dual-endpoint em `S3ClientConfig.java`: o cliente S3 interno continua conectando em `http://minio:9000`, enquanto o `S3Presigner` assina para `AWS_S3_PRESIGNED_ENDPOINT: http://localhost:9000`.
  2. Implementação do endpoint de streaming direto `GET /api/v1/documentos/{id}/conteudo` em `FicheiroDigitalController.java` para garantir transmissão de bytes não bloqueante independente de topologia de rede.
  3. No frontend (`DocumentoPreviewModalComponent.ts`), adicionado tratamento que substitui dinamicamente o hostname interno e aciona o fallback direto de conteúdo se necessário.

### 6.3 Aliases do MinIO Client (`mc`)
- **Problema:** Comandos executados com `/usr/bin/mc cp ... localminio/...` tratavam `localminio` como pasta física no container porque o alias configurado no container oficial do MinIO é `local` (`http://localhost:9000`).
- **Resolução:** Padronizado o uso do alias `local/govflow-documentos/...`, garantindo upload direto ao bucket e expurgo de arquivos temporários.

---

## 7. Próximos Passos (Transição para o EPIC 3)

Com a espinha dorsal de GED e Ficheiro Digital consolidada, livre de mocks e validada visualmente na interface, o sistema avança para o **EPIC 3: Módulo 3 — Comunicação Omnicanal, WhatsApp & IA de Contexto**:
1. Implementação do `TICKET-WPP-01` (Migração Flyway `V21` para contatos 1:N e caixa de triagem).
2. Implementação do `TICKET-WPP-02` e `TICKET-WPP-03` (Ingestão agnóstica no `whatsapp-service` e janela de contexto de 5 mensagens na IA).
3. Implementação do `TICKET-WPP-04` ao `TICKET-WPP-06` (Caixa de triagem `/triagem` e Quick Drawer de cadastro de contatos).
