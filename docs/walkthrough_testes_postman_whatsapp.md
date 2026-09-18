# Walkthrough de Testes: GovFlow (WhatsApp & Postman)

Este documento orienta a realização de testes ponta a ponta na plataforma **GovFlow**, detalhando o fluxo operacional tanto através do **WhatsApp em tempo real** quanto através da **Postman Collection** com cobertura de todos os cenários felizes e *edge cases* fiscais e arquiteturais.

---

## 1. Visão Geral da Arquitetura & Fluxo de Dados

O GovFlow processa documentos fiscais (Notas Fiscais de Serviços, Boletins de Medição, Recibos) através de um pipeline assíncrono e orientado a eventos (*Event-Driven Architecture*):

```mermaid
flowchart TD
    subgraph Ingestao ["1. Ingestão WhatsApp"]
        A["📱 Smartphone do Usuário"] -->|Envia PDF / Foto NF| B["Evolution API (:8084)"]
        B -->|Webhook HTTP POST| C["whatsapp-service (:8083)"]
        C -->|Upload S3 Streaming| D[("MinIO (:9000)\nBucket: govflow-documents")]
        C -->|Publish: DocumentoRecebidoEvent| E{{"RabbitMQ (:5672)\nExchange: govflow.events"}}
    end

    subgraph IA ["2. Extração Multimodal"]
        E -->|Queue: fila.documentos.extrair| F["ai-service (:8000)\nFastAPI + Gemini Multimodal"]
        F -->|Download S3| D
        F -->|Processa OCR / Bounding Boxes / Validação| F
        F -->|Publish: DocumentoExtraidoEvent| E
    end

    subgraph Core ["3. Agregado & Auditoria"]
        E -->|Queue: fila.documentos.processados| G["core-service (:8081)\nHexagonal / DDD"]
        G -->|Persistência EM_CONFERENCIA| H[("PostgreSQL 16 (:5432)\ncore_schema.tb_documentos")]
    end

    subgraph Humano ["4. Revisão Human-in-the-Loop"]
        I["👨‍💼 Analista Fiscal (Postman / Frontend)"] -->|PUT /api/v1/documentos/{id}/aprovar| G
        G -->|Auditoria com Diff| J[("core_schema.tb_auditorias_revisao")]
        G -->|Publish: DocumentoProntoParaTransferegovEvent| E
    end
```

---

## 2. Como Testar via WhatsApp (Tempo Real)

### Pré-requisitos
1. Todos os containers da stack rodando: `docker compose ps`
2. Instância do WhatsApp conectada na Evolution API (`govflow-consultoria`).

---

### Passo 1: Verificar se o WhatsApp está Pareado

Execute no terminal ou consulte no navegador a API da Evolution:

```bash
curl -X GET "http://localhost:8084/instance/connectionState/govflow-consultoria" \
  -H "apikey: GovFlowSuperSecretApiKey2026"
```

> **Resposta esperada:** `{"instance": {"state": "open"}}`. Se estiver `close`, abra a interface web ou leia o QR Code gerado pelo endpoint `/instance/connect/govflow-consultoria`.

---

### Passo 2: Cadastrar o Contato da Prefeitura (Vínculo de Tenant)

Para que a mensagem do WhatsApp seja vinculada a uma consultoria cliente e prefeitura correspondente, o telefone do remetente precisa constar na tabela de contatos:

```sql
-- Exemplo vinculando o telefone do remetente (com DDI e DDD, ex: 5583999998888)
INSERT INTO whatsapp_schema.tb_contatos_prefeitura 
(id, tenant_id, prefeitura_id, nome_contato, cargo, telefone_e164, ativo)
VALUES 
(gen_random_uuid(), 
 'f679b2d5-70d7-4efa-a6d8-b550a4594032', -- Substitua pelo ID da sua consultoria (Tenant)
 gen_random_uuid(), 
 'Secretário de Obras', 
 'Secretário', 
 '5583999998888', -- Seu número com 55 + DDD + Telefone
 true);
```
*(Se não cadastrar, o `whatsapp-service` tratará como contato desconhecido `unidentified`, gravando a mídia e gerando evento mesmo assim).*

---

### Passo 3: Enviar o Documento pelo WhatsApp

1. No seu celular, abra a conversa com o número conectado ao **GovFlow**.
2. Envie uma foto ou arquivo PDF de uma **Nota Fiscal de Serviços** ou documento contábil.
3. Acompanhe o processamento nos logs dos containers:

```bash
# Terminal 1: Ingestão e streaming para o S3
docker compose logs -f whatsapp-service

# Terminal 2: Extração via Gemini AI
docker compose logs -f ai-service

# Terminal 3: Criação do documento no Core Service
docker compose logs -f core-service
```

---

### Passo 4: O Que Você Verá Acontecer

1. **`govflow-whatsapp-service`**:
   - Recebe o webhook `messages.upsert` da Evolution API.
   - Faz o download da mídia e executa *streaming* direto para o bucket `govflow-documents` no MinIO (sem salvar nada em disco).
   - Registra a mensagem na tabela `whatsapp_schema.tb_mensagens_inbound`.
   - Publica o evento `DocumentoRecebidoEvent` na exchange `govflow.events`.

2. **`govflow-ai-service`**:
   - Consome a mensagem da fila `fila.documentos.extrair`.
   - Baixa o binário do MinIO e submete ao modelo multimodal (Gemini Flash / Gemma).
   - Extrai tipo de documento, número, datas, CNPJ, razão social, valores fiscais (bruto, retenções INSS/ISS/IRRF, líquido) e caixas delimitadoras (*bounding boxes*).
   - Publica o evento `DocumentoExtraidoEvent` na exchange `govflow.events` com routing key `documento.extraido`.

3. **`govflow-core-service`**:
   - O `DocumentoProcessadoListener` consome o evento da fila `fila.documentos.processados`.
   - Popula o agregado `Documento` no estado `EM_CONFERENCIA`.
   - O documento fica disponível na API REST (`GET /api/v1/documentos`) pronto para validação pelo analista.

---

## 3. Como Testar via Postman (Coleção de Edge Cases)

A suíte completa conta com **21 requisições encadeadas** que testam desde o *happy path* até tentativas de invasão *cross-tenant* e violações fiscais de tolerância zero contábil.

### Arquivo da Coleção
- [`docs/postman/GovFlow_Task06_EdgeCases.postman_collection.json`](file:///c:/projetos/estudo%20spring/govflow/docs/postman/GovFlow_Task06_EdgeCases.postman_collection.json)

---

### Importação e Execução

1. No Postman, clique em **Import** e escolha o arquivo `GovFlow_Task06_EdgeCases.postman_collection.json`.
2. As variáveis de ambiente da coleção são populadas automaticamente conforme a execução:
   - `baseUrl`: `http://localhost:8081` (ou `http://localhost:8080` via Gateway)
   - `rabbitMqUrl`: `http://localhost:15672`
   - `tenantId`: Capturado na criação da Consultoria A
   - `tenantBId`: Capturado na criação da Consultoria B
   - `documentoId`: Capturado na criação/ingestão
   - `analistaId`: Gerado dinamicamente via UUID

---

### Roteiro dos Testes por Pasta

#### Pasta 01 — Setup & Multi-Tenancy
- **`01 - Cadastrar Consultoria A (Tenant Principal)`**: Registra a Consultoria Alpha (`12.345.678/0001-95`) e salva o `tenantId`. Retorna `201 Created`.
- **`02 - Cadastrar Consultoria B (Tenant Concorrente)`**: Registra a Consultoria Beta (`98.765.432/0001-98`) e salva o `tenantBId`. Retorna `201 Created`.

#### Pasta 02 — Ingestão AMQP & Edge Cases de Mensageria
- **`03 - RabbitMQ: Publicar Documento Extraído Válido (Tenant A)`**: Simula a chegada de uma NF processada pelo AI Service via RabbitMQ Management API (`routing_key: documento.extraido`).
- **`04 - RabbitMQ Edge Case: Mensagem sem TenantId (Poison Pill)`**: Publica mensagem corrompida sem `tenantId`. O listener rejeita sem requeue (`AmqpRejectAndDontRequeueException`), desviando a mensagem imediatamente para `fila.documentos.processados.dlq`.

#### Pasta 03 — Consultas & Blindagem Multi-Tenant
- **`05 - GET Consultar Documento por ID (Happy Path)`**: Retorna o documento em `EM_CONFERENCIA` com valores fiscais, scores de confiança e coordenadas de bounding boxes.
- **`06 - GET Edge Case: Ausência de Header X-Tenant-Id`**: Retorna `400 Bad Request` no formato RFC 7807 (`missing-tenant`).
- **`07 - GET Edge Case: Header X-Tenant-Id Malformado`**: Envia ID que não é UUID válido. Retorna `400 Bad Request` (`invalid-tenant`).
- **`08 - GET Edge Case: Tentativa de Invasão Cross-Tenant`**: Tenant B tenta acessar o documento do Tenant A. Retorna `404 Not Found` (isolamento absoluto entre prefeituras).
- **`09 - GET Listar Documentos Paginado no Tenant A`**: Lista paginada com filtro de status. Retorna `totalElements >= 1`.
- **`10 - GET Listar Documentos no Tenant B (Zero Leakage)`**: Tenant B lista seus documentos e recebe `totalElements: 0` (zero vazamento de dados).

#### Pasta 04 — Ciclo de Aprovação Auditada & Edge Cases Fiscais
- **`11 - PUT Edge Case: Aprovação sem AnalistaId`**: Tenta aprovar sem analista. Retorna `400 Bad Request` (exigência de não-repúdio).
- **`12 - PUT Edge Case: CNPJ do Credor com DV Inválido`**: Tenta aprovar com CNPJ matematicamente inválido. Retorna `422 Unprocessable` (`CNPJ_INVALIDO`).
- **`13 - PUT Edge Case: Inconsistência Matemática ($Bruto - Deducoes \neq Liquido$)`**: Informa Bruto 10.000,00, Deduções 600,00 e Líquido 8.900,00. Retorna `422 Unprocessable` com diagnóstico detalhado no RFC 7807 (`diferenca: 500.00`, `tolerancia: 0.00`).
- **`14 - PUT Edge Case: Divergência na Soma das Retenções`**: A soma das retenções individuais (INSS + ISS) não bate com `valorTotalDeducoes`. Retorna `422 Unprocessable`.
- **`15 - PUT Aprovar Documento com Sucesso (Happy Path)`**: Analista aprova com precisão decimal, transiciona para `PRONTO_PARA_TRANSFEREGOV` e publica `DocumentoProntoParaTransferegovEvent` pós-commit do banco. Retorna `200 OK`.
- **`16 - PUT Edge Case: Transição de Estado Inválida`**: Tenta reaprovar documento já finalizado. Retorna `422 Unprocessable` (`DOCUMENTO_ESTADO_INVALIDO`).

#### Pasta 05 — Trilha de Auditoria & Diff
- **`17 - GET Consultar Auditoria com Diff de Campos Alterados`**: Retorna a lista de auditorias contendo ação `APROVACAO`, timestamp, analista, e o comparativo `diff` campo a campo entre o que a IA extraiu e o que o analista corrigiu.

#### Pasta 06 — Fluxo de Rejeição & Idempotência
- **`18 - RabbitMQ: Ingerir Segundo Documento`**: Publica um segundo documento para o Tenant A no RabbitMQ.
- **`19 - PUT Edge Case: Rejeição sem Justificativa`**: Tenta rejeitar com motivo vazio. Retorna `400 Bad Request`.
- **`20 - PUT Rejeitar Documento com Justificativa`**: Analista rejeita com fundamentação. Transiciona para `REJEITADO` e registra auditoria de rejeição.
- **`21 - RabbitMQ Edge Case: Redelivery Idempotente`**: RabbitMQ reentrega evento para o documento já rejeitado. O listener detecta estado final e faz no-op sem falhar a fila.

---

## 4. Dicas de Diagnóstico e Comandos Úteis

| Ação | Comando |
| :--- | :--- |
| **Limpar base para novos testes** | `docker exec govflow-postgres psql -U postgres -d govflow -c "TRUNCATE TABLE core_schema.tb_consultorias, core_schema.tb_documentos, core_schema.tb_auditorias_revisao CASCADE;"` |
| **Expurgar filas RabbitMQ** | `docker exec govflow-rabbitmq rabbitmqctl purge_queue fila.documentos.processados` |
| **Verificar mensagens na DLQ** | `docker exec govflow-rabbitmq rabbitmqctl list_queues name messages` |
| **Consultar auditorias no banco** | `docker exec govflow-postgres psql -U postgres -d govflow -c "SELECT id, acao, analista_id, diff_alteracoes_json FROM core_schema.tb_auditorias_revisao;"` |
